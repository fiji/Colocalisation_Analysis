/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2023 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package sc.fiji.coloc.results;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.io.SaveDialog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import sc.fiji.coloc.algorithms.AutoThresholdRegression;
import sc.fiji.coloc.algorithms.Histogram2D;
import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.gadgets.DataContainer.MaskType;


public class PDFWriter<T extends RealType<T>> implements ResultHandler<T> {

	// indicates if we want to produce US letter or A4 size
	boolean isLetter = false;
	// indicates if the content is the first item on the page
	boolean isFirst  = true;
	// show the name of the image
	static boolean showName=true;
	// a static counter for this sessions created PDFs
	static int succeededPrints = 0;
	// show the size in pixels of the image
	static boolean showSize=true;
	// a reference to the data container
	DataContainer<T> container;
	PdfWriter writer;
	Document document;

	// a list of the available result images, no matter what specific kinds
	protected List<com.itextpdf.text.Image> listOfPDFImages
		= new ArrayList<com.itextpdf.text.Image>();
	protected List<Paragraph> listOfPDFTexts
		= new ArrayList<Paragraph>();
	// a list of PDF warnings
	protected List<Paragraph> PDFwarnings = new ArrayList<Paragraph>();

	/**
	 * Creates a new PDFWriter that can access the container.
	 *
	 * @param container The data container for source image data
	 */
	public PDFWriter(DataContainer<T> container) {
		this.container = container;
	}

	@Override
	public void handleImage(RandomAccessibleInterval<T> image, String name) {
		ImagePlus imp = ImageJFunctions.wrapFloat( image, name );

		// set the display range
		double max = ImageStatistics.getImageMax(image).getRealDouble();
		imp.setDisplayRange(0.0, max);
		addImageToList(imp, name);
	}

	/**
	 * Handles a histogram the following way: create snapshot, log data, reset the
	 * display range, apply the Fire LUT and finally store it as an iText PDF image.
	 * Afterwards the image is reset to its orignal state again
	 */
	@Override
	public void handleHistogram(Histogram2D<T> histogram, String name) {
		RandomAccessibleInterval<LongType> image = histogram.getPlotImage();
		ImagePlus imp = ImageJFunctions.wrapFloat( image, name );
		
		// make a snapshot to be able to reset after modifications
		imp.getProcessor().snapshot();
		imp.getProcessor().log();
		imp.updateAndDraw();
		imp.getProcessor().resetMinAndMax();
		IJ.run(imp,"Fire", null);
		
		Overlay overlay = new Overlay();
		
		/*
		 * check if we should draw a regression line for the current
		 * histogram.
		 */
		if (histogram.getDrawingSettings().contains(Histogram2D.DrawingFlags.RegressionLine)) {
			AutoThresholdRegression<T> autoThreshold = this.container.getAutoThreshold();
			if (histogram != null && autoThreshold != null) {
				drawLine(histogram, overlay, image.dimension(0), image.dimension(1),
						autoThreshold.getAutoThresholdSlope(), autoThreshold.getAutoThresholdIntercept());
				overlay.setStrokeColor(java.awt.Color.WHITE);
				imp.setOverlay(overlay);
			}
		}
		
		addImageToList(imp, name);
		// reset the imp from the log scaling we applied earlier
		imp.getProcessor().reset();
	}

	private void drawLine(Histogram2D<T> histogram, Overlay overlay, long imgWidth, long imgHeight, double slope,
			double intercept) {

		double startX, startY, endX, endY;
		/*
		 * since we want to draw the line over the whole image we can directly
		 * use screen coordinates for x values.
		 */
		startX = 0.0;
		endX = imgWidth;

		// check if we can get some exta information for drawing
		// get calibrated start y coordinates
		double calibratedStartY = slope * histogram.getXMin() + intercept;
		double calibratedEndY = slope * histogram.getXMax() + intercept;
		// convert calibrated coordinates to screen coordinates
		startY = calibratedStartY * histogram.getYBinWidth();
		endY = calibratedEndY * histogram.getYBinWidth();

		/*
		 * since the screen origin is in the top left of the image, we need to
		 * x-mirror our line
		 */
		startY = (imgHeight - 1) - startY;
		endY = (imgHeight - 1) - endY;
		// create the line ROI and add it to the overlay
		Line lineROI = new Line(startX, startY, endX, endY);
		/*
		 * Set drawing width of line to one, in case it has been changed
		 * globally.
		 */
		lineROI.setStrokeWidth(1.0f);
		overlay.add(lineROI);
		
	}
	
	protected void addImageToList(ImagePlus imp, String name) {
		
		Overlay overlay = imp.getOverlay();
		IJ.log("overlay="+overlay);
		
		if(overlay!=null && overlay.size()>0)
			IJ.log(""+overlay.get(0));
		
		ImagePlus flatten = imp.flatten();
		//flatten.setTitle("FlatteTest");
		//flatten.show();
		
		java.awt.Image awtImage = flatten.getImage();
		
		try {
			com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(awtImage, null);
			pdfImage.setAlt(name); // iText-1.3 setMarkupAttribute("name", name); 
			listOfPDFImages.add(pdfImage);
		}
		catch (BadElementException e) {
			IJ.log("Could not convert image to correct format for PDF generation");
			IJ.handleException(e);
		}
		catch (IOException e) {
			IJ.log("Could not convert image to correct format for PDF generation");
			IJ.handleException(e);
		}
	}

	@Override
	public void handleWarning(Warning warning) {
		PDFwarnings.add(new Paragraph("Warning! " + warning.getShortMessage() + " - " + warning.getLongMessage()));
	}

	@Override
	public void handleValue(String name, String value) {
		listOfPDFTexts.add(new Paragraph(name + ": " + value));
	}

	@Override
	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	@Override
	public void handleValue(String name, double value, int decimals) {
		listOfPDFTexts.add(new Paragraph(name + ": " + IJ.d2s(value, decimals)));
	}

	/**
	 * Prints an image into the opened PDF.
	 * @param image The image to print.
	 * @param xLabel 
	 * @param yLabel 
	 */
	protected void addImage(com.itextpdf.text.Image image, String xLabel, String yLabel)
			throws DocumentException, IOException {

		if (! isFirst) {
			document.add(new Paragraph("\n"));
			float vertPos = writer.getVerticalPosition(true);
			if (vertPos - document.bottom() < image.getHeight()) {
				document.newPage();
			} else {
				PdfContentByte cb = writer.getDirectContent();
				cb.setLineWidth(1f);
				if (isLetter) {
					cb.moveTo(PageSize.LETTER.getLeft(50), vertPos);
					cb.lineTo(PageSize.LETTER.getRight(50), vertPos);
				} else {
					cb.moveTo(PageSize.A4.getLeft(50), vertPos);
					cb.lineTo(PageSize.A4.getRight(50), vertPos);
				}
				cb.stroke();
			}
		}

		if (showName) {
			Paragraph paragraph = new Paragraph(image.getAlt()); // iText-1.3: getMarkupAttribute("name"));
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcNm = 40;
		}

		if (showSize) {
			Paragraph paragraph = new Paragraph(image.getWidth() + " x " + image.getHeight());
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcSz = 40;
		}
		//adding label to the image as a table
		PdfPTable table = new PdfPTable(2);
	    table.setWidthPercentage(85);
		table.setWidths(new int[]{1, 2});
		table.setSpacingBefore(4f);
		
		// adding first row with y-axis and the image 
		PdfPCell cell = new PdfPCell(new Phrase("\tChannel 2\n" + labelName(yLabel)));
		cellStyle(cell);
		table.addCell(cell);
	    image.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
	    table.addCell((image));
	    
	    // adding second row with an empty cell and x-axis
	    cell = new PdfPCell();
		cellStyle(cell);
		table.addCell(cell);
	    cell = new PdfPCell(new Phrase("\tChannel 1\n" + labelName(xLabel)));
		cellStyle(cell);
		table.addCell(cell);
		document.add(table);

		isFirst = false;
	}

	private void cellStyle(PdfPCell cell) {
		//alignment
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		
		//padding
		cell.setPaddingTop(2f);
				
		//border
		cell.setBorder(0);
		cell.setBorderColor(BaseColor.WHITE);
		
	}

	private String labelName(String s) {
		// TODO Auto-generated method stub
		final int maxLen = 30;
		return s.length() > maxLen ? //
				s.substring(0, maxLen - 3) + "..." : s;
	}

	@Override
	public void process() {
		try {
			// Use the getJobName() in DataContainer for the job name.
			String jobName = container.getJobName();

			/* If a mask is in use, add a counter
			 * information to the jobName.
			 */
			if (container.getMaskType() != MaskType.None) {
				// maskHash is now used as the mask or ROI unique ID in the
				// jobName but we can still increment and use succeededPrints at
				// the end of the filename for PDFs when there is a mask.
				jobName += (succeededPrints + 1);
			}
			// get the path to the file we are about to create
			SaveDialog sd = new SaveDialog("Save as PDF", jobName, ".pdf");
			// update jobName if the user changes it in the save file dialog.
			jobName = sd.getFileName();
			String directory = sd.getDirectory();
			// make sure we have what we need next
			if ((jobName == null) || (directory == null)) {
				return;
			}
			String path = directory+jobName;
			// create a new iText Document and add date and title
			document = new Document(isLetter ? PageSize.LETTER : PageSize.A4);
			document.addCreationDate();
			document.addTitle(jobName);
			// get a writer object to do the actual output
			writer = PdfWriter.getInstance(document, new FileOutputStream(path));
			document.open();

			// write job name at the top of the PDF file as a title
			Paragraph titlePara = new Paragraph(jobName);
			document.add(titlePara);

			// iterate over all produced images
			for (com.itextpdf.text.Image img : listOfPDFImages) {
				addImage(img, container.getSourceCh1Name(), container.getSourceCh2Name());
			}

			//iterate over all produced PDFwarnings
			for (Paragraph p : PDFwarnings) {
				document.add(p);
			}

			//iterate over all produced text objects
			for (Paragraph p : listOfPDFTexts) {
				document.add(p);
			}
		} catch(DocumentException de) {
			IJ.showMessage("PDF Writer", de.getMessage());
		} catch(IOException ioe) {
			IJ.showMessage("PDF Writer", ioe.getMessage());
		} finally {
			if (document !=null) {
				document.close();
				succeededPrints++;
			}
		}
	}
}
