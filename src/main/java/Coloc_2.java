/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2017 Fiji developers.
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

import java.awt.Checkbox;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import algorithms.Algorithm;
import algorithms.AutoThresholdRegression;
import algorithms.AutoThresholdRegression.Implementation;
import algorithms.CostesSignificanceTest;
import algorithms.Histogram2D;
import algorithms.InputCheck;
import algorithms.KendallTauRankCorrelation;
import algorithms.LiHistogram2D;
import algorithms.LiICQ;
import algorithms.MandersColocalization;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.SpearmanRankCorrelation;
import fiji.Debug;
import gadgets.DataContainer;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import results.PDFWriter;
import results.ResultHandler;
import results.SingleWindowDisplay;
import results.Warning;

/**
 * An ImageJ plugin which does pixel intensity correlation based colocalisation
 * analysis on a pair of images, with optional Mask or ROI.
 *
 * @param <T>
 * @author Daniel J. White
 * @author Tom Kazimiers
 * @author Johannes Schindelin
 */
public class Coloc_2<T extends RealType<T> & NativeType<T>> implements PlugIn {

	// a small bounding box container
	protected class BoundingBox {

		public long[] offset;
		public long[] size;

		public BoundingBox(final long[] offset, final long[] size) {
			this.offset = offset.clone();
			this.size = size.clone();
		}
	}

	// a storage class for ROI information
	protected class MaskInfo {

		BoundingBox roi;
		public Img<T> mask;

		// constructors
		public MaskInfo(final BoundingBox roi, final Img<T> mask) {
			this.roi = roi;
			this.mask = mask;
		}

		public MaskInfo() {}
	}

	// the storage key for Fiji preferences
	protected final static String PREF_KEY = "Coloc_2.";

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {
			None, Img1, Img2, Mask, RoiManager
	};

	// the ROI configuration to use
	protected RoiConfiguration roiConfig = RoiConfiguration.Img1;

	// A list of all ROIs/masks found
	protected ArrayList<MaskInfo> masks = new ArrayList<>();

	// A list of auto threshold implementations
	protected String[] regressions =
		new String[AutoThresholdRegression.Implementation.values().length];

	// default indices of image, mask, ROI and regression choices
	protected static int index1 = 0;
	protected static int index2 = 1;
	protected static int indexMask = 0;
	protected static int indexRoi = 0;
	protected static int indexRegr = 0;

	// the images to work on
	protected Img<T> img1, img2;

	// names of the images working on
	protected String Ch1Name = "";
	protected String Ch2Name = "";

	// the channels of the images to use
	protected int img1Channel = 1, img2Channel = 1;

	/* The different algorithms this plug-in provides.
	 * If a reference is null it will not get run.
	 */
	protected PearsonsCorrelation<T> pearsonsCorrelation;
	protected LiHistogram2D<T> liHistogramCh1;
	protected LiHistogram2D<T> liHistogramCh2;
	protected LiICQ<T> liICQ;
	protected SpearmanRankCorrelation<T> SpearmanRankCorrelation;
	protected MandersColocalization<T> mandersCorrelation;
	protected KendallTauRankCorrelation<T> kendallTau;
	protected Histogram2D<T> histogram2D;
	protected CostesSignificanceTest<T> costesSignificance;
	// indicates if images should be printed in result
	protected boolean displayImages;

	// indicates if a PDF should be saved automatically
	protected boolean autoSavePdf;

	@Override
	public void run(final String arg0) {
		if (showDialog()) {
			try {
				for (final MaskInfo mi : masks) {
					colocalise(img1, img2, mi.roi, mi.mask);
				}
			}
			catch (final MissingPreconditionException e) {
				IJ.handleException(e);
				IJ.showMessage("An error occured, could not colocalize!");
				return;
			}
		}
	}

	public boolean showDialog() {
		// get IDs of open windows
		final int[] windowList = WindowManager.getIDList();
		// if there are less than 2 windows open, cancel
		if (windowList == null || windowList.length < 2) {
			IJ.showMessage("At least 2 images must be open!");
			return false;
		}

		/* create a new generic dialog for the
		 * display of various options.
		 */
		final GenericDialog gd = new GenericDialog("Coloc 2");

		final String[] titles = new String[windowList.length];
		/* the masks and ROIs array needs three more entries than
		 * windows to contain "none", "ROI ch 1" and "ROI ch 2"
		 */
		final String[] roisAndMasks = new String[windowList.length + 4];
		roisAndMasks[0] = "<None>";
		roisAndMasks[1] = "ROI(s) in channel 1";
		roisAndMasks[2] = "ROI(s) in channel 2";
		roisAndMasks[3] = "ROI Manager";

		// go through all open images and add them to GUI
		for (int i = 0; i < windowList.length; i++) {
			final ImagePlus imp = WindowManager.getImage(windowList[i]);
			if (imp != null) {
				titles[i] = imp.getTitle();
				roisAndMasks[i + 4] = imp.getTitle();
			}
			else {
				titles[i] = "";
			}
		}

		// find all available regression strategies
		final Implementation[] regressionImplementations =
			AutoThresholdRegression.Implementation.values();
		for (int i = 0; i < regressionImplementations.length; ++i) {
			regressions[i] = regressionImplementations[i].toString();
		}

		// set up the users preferences
		displayImages = Prefs.get(PREF_KEY + "displayImages", false);
		autoSavePdf = Prefs.get(PREF_KEY + "autoSavePdf", true);
		boolean displayShuffledCostes = Prefs.get(PREF_KEY +
			"displayShuffledCostes", false);
		boolean useLiCh1 = Prefs.get(PREF_KEY + "useLiCh1", true);
		boolean useLiCh2 = Prefs.get(PREF_KEY + "useLiCh2", true);
		boolean useLiICQ = Prefs.get(PREF_KEY + "useLiICQ", true);
		boolean useSpearmanRank = Prefs.get(PREF_KEY + "useSpearmanRank", true);
		boolean useManders = Prefs.get(PREF_KEY + "useManders", true);
		boolean useKendallTau = Prefs.get(PREF_KEY + "useKendallTau", true);
		boolean useScatterplot = Prefs.get(PREF_KEY + "useScatterplot", true);
		boolean useCostes = Prefs.get(PREF_KEY + "useCostes", true);
		int psf = (int) Prefs.get(PREF_KEY + "psf", 3);
		int nrCostesRandomisations = (int) Prefs.get(PREF_KEY +
			"nrCostesRandomisations", 10);
		indexRegr = (int) Prefs.get(PREF_KEY + "regressionImplementation", 0);

		/* make sure the default indices are no bigger
		 * than the amount of images we have
		 */
		index1 = clip(index1, 0, titles.length);
		index2 = clip(index2, 0, titles.length);
		indexMask = clip(indexMask, 0, roisAndMasks.length - 1);

		gd.addChoice("Channel_1", titles, titles[index1]);
		gd.addChoice("Channel_2", titles, titles[index2]);
		gd.addChoice("ROI_or_mask", roisAndMasks, roisAndMasks[indexMask]);
		// gd.addChoice("Use ROI", roiLabels, roiLabels[indexRoi]);

		gd.addChoice("Threshold_regression", regressions, regressions[indexRegr]);

		gd.addCheckbox("Show_Save_PDF_Dialog", autoSavePdf);
		gd.addCheckbox("Display_Images_in_Result", displayImages);
		gd.addCheckbox("Display_Shuffled_Images", displayShuffledCostes);
		final Checkbox shuffleCb = (Checkbox) gd.getCheckboxes().lastElement();
		// Add algorithm options
		gd.addMessage("Algorithms:");
		gd.addCheckbox("Li_Histogram_Channel_1", useLiCh1);
		gd.addCheckbox("Li_Histogram_Channel_2", useLiCh2);
		gd.addCheckbox("Li_ICQ", useLiICQ);
		gd.addCheckbox("Spearman's_Rank_Correlation", useSpearmanRank);
		gd.addCheckbox("Manders'_Correlation", useManders);
		gd.addCheckbox("Kendall's_Tau_Rank_Correlation", useKendallTau);
		gd.addCheckbox("2D_Instensity_Histogram", useScatterplot);
		gd.addCheckbox("Costes'_Significance_Test", useCostes);
		final Checkbox costesCb = (Checkbox) gd.getCheckboxes().lastElement();
		gd.addNumericField("PSF", psf, 1);
		gd.addNumericField("Costes_randomisations", nrCostesRandomisations, 0);

		// disable shuffle checkbox if costes checkbox is set to "off"
		shuffleCb.setEnabled(useCostes);
		costesCb.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				shuffleCb.setEnabled(costesCb.getState());
			}
		});

		// show the dialog, finally
		gd.showDialog();
		// do nothing if dialog has been canceled
		if (gd.wasCanceled()) return false;

		final ImagePlus imp1 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
		final ImagePlus imp2 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);

		// get image names for output
		Ch1Name = imp1.getTitle();
		Ch2Name = imp2.getTitle();

		// make sure both images have the same bit-depth
		if (imp1.getBitDepth() != imp2.getBitDepth()) {
			IJ.showMessage("Both images must have the same bit-depth.");
			return false;
		}

		// get information about the mask/ROI to use
		indexMask = gd.getNextChoiceIndex();
		if (indexMask == 0) roiConfig = RoiConfiguration.None;
		else if (indexMask == 1) roiConfig = RoiConfiguration.Img1;
		else if (indexMask == 2) roiConfig = RoiConfiguration.Img2;
		else if (indexMask == 3) roiConfig = RoiConfiguration.RoiManager;
		else {
			roiConfig = RoiConfiguration.Mask;
			/* Make indexMask the reference to the mask image to use.
			 * To do this we reduce it by three for the first three
			 * entries in the combo box.
			 */
			indexMask = indexMask - 4;
		}

		// save the ImgLib wrapped images as members
		img1 = ImagePlusAdapter.wrap(imp1);
		img2 = ImagePlusAdapter.wrap(imp2);

		/* check if we have a valid ROI for the selected configuration
		 * and if so, get the ROI's bounds. Alternatively, a mask can
		 * be selected (that is basically all, but a rectangle).
		 */
		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
			createMasksFromImage(imp1);
		}
		else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
			createMasksFromImage(imp2);
		}
		else if (roiConfig == RoiConfiguration.RoiManager) {
			if (!createMasksFromRoiManager(imp1.getWidth(), imp1.getHeight()))
				return false;
		}
		else if (roiConfig == RoiConfiguration.Mask) {
			// get the image to be used as mask
			final ImagePlus maskImp = WindowManager.getImage(windowList[indexMask]);
			final Img<T> maskImg = ImagePlusAdapter.<T> wrap(maskImp);
			// get a valid mask info for the image
			final MaskInfo mi = getBoundingBoxOfMask(maskImg);
			masks.add(mi);
		}
		else {
			/* if no ROI/mask is selected, just add an empty MaskInfo
			 * to colocalise both images without constraints.
			 */
			masks.add(new MaskInfo(null, null));
		}

		// get information about the mask/ROI to use
		indexRegr = gd.getNextChoiceIndex();

		// read out GUI data
		autoSavePdf = gd.getNextBoolean();
		displayImages = gd.getNextBoolean();
		displayShuffledCostes = gd.getNextBoolean();
		useLiCh1 = gd.getNextBoolean();
		useLiCh2 = gd.getNextBoolean();
		useLiICQ = gd.getNextBoolean();
		useSpearmanRank = gd.getNextBoolean();
		useManders = gd.getNextBoolean();
		useKendallTau = gd.getNextBoolean();
		useScatterplot = gd.getNextBoolean();
		useCostes = gd.getNextBoolean();
		psf = (int) gd.getNextNumber();
		nrCostesRandomisations = (int) gd.getNextNumber();

		// save user preferences
		Prefs.set(PREF_KEY + "regressionImplementation", indexRegr);
		Prefs.set(PREF_KEY + "autoSavePdf", autoSavePdf);
		Prefs.set(PREF_KEY + "displayImages", displayImages);
		Prefs.set(PREF_KEY + "displayShuffledCostes", displayShuffledCostes);
		Prefs.set(PREF_KEY + "useLiCh1", useLiCh1);
		Prefs.set(PREF_KEY + "useLiCh2", useLiCh2);
		Prefs.set(PREF_KEY + "useLiICQ", useLiICQ);
		Prefs.set(PREF_KEY + "useSpearmanRank", useSpearmanRank);
		Prefs.set(PREF_KEY + "useManders", useManders);
		Prefs.set(PREF_KEY + "useKendallTau", useKendallTau);
		Prefs.set(PREF_KEY + "useScatterplot", useScatterplot);
		Prefs.set(PREF_KEY + "useCostes", useCostes);
		Prefs.set(PREF_KEY + "psf", psf);
		Prefs.set(PREF_KEY + "nrCostesRandomisations", nrCostesRandomisations);

		// Parse algorithm options
		pearsonsCorrelation = new PearsonsCorrelation<>(
			PearsonsCorrelation.Implementation.Fast);

		if (useLiCh1) liHistogramCh1 = new LiHistogram2D<>("Li - Ch1", true);
		if (useLiCh2) liHistogramCh2 = new LiHistogram2D<>("Li - Ch2", false);
		if (useLiICQ) liICQ = new LiICQ<>();
		if (useSpearmanRank) {
			SpearmanRankCorrelation = new SpearmanRankCorrelation<>();
		}
		if (useManders) mandersCorrelation = new MandersColocalization<>();
		if (useKendallTau) kendallTau = new KendallTauRankCorrelation<>();
		if (useScatterplot) histogram2D = new Histogram2D<>(
			"2D intensity histogram");
		if (useCostes) {
			costesSignificance = new CostesSignificanceTest<>(pearsonsCorrelation,
				psf, nrCostesRandomisations, displayShuffledCostes);
		}

		return true;
	}

	/**
	 * Call this method to run a whole colocalisation configuration, all selected
	 * algorithms get run on the supplied images. You can specify the data further
	 * by supplying appropriate information in the mask structure.
	 *
	 * @param img1
	 * @param img2
	 * @param roi
	 * @param mask
	 * @throws MissingPreconditionException
	 */
	public void colocalise(final Img<T> img1, final Img<T> img2,
		final BoundingBox roi, final Img<T> mask)
		throws MissingPreconditionException
	{
		colocalise(img1, img2, roi, mask, null);
	}

	/**
	 * Call this method to run a whole colocalisation configuration, all selected
	 * algorithms get run on the supplied images. You can specify the data further
	 * by supplying appropriate information in the mask structure.
	 *
	 * @param img1 First image.
	 * @param img2 Second image.
	 * @param roi Region of interest to which analysis is confined.
	 * @param mask Mask to which analysis is confined.
	 * @param extraHandlers additional objects to be notified of analysis results.
	 * @throws MissingPreconditionException
	 */
	public void colocalise(final Img<T> img1, final Img<T> img2,
		final BoundingBox roi, final Img<T> mask,
		final List<ResultHandler<T>> extraHandlers)
		throws MissingPreconditionException
	{
		// create a new container for the selected images and channels
		DataContainer<T> container;
		if (mask != null) {
			container = new DataContainer<>(img1, img2, img1Channel, img2Channel,
				Ch1Name, Ch2Name, mask, roi.offset, roi.size);
		}
		else if (roi != null) {
			// we have no mask, but a regular ROI in use
			container = new DataContainer<>(img1, img2, img1Channel, img2Channel,
				Ch1Name, Ch2Name, roi.offset, roi.size);
		}
		else {
			// no mask and no ROI is present
			container = new DataContainer<>(img1, img2, img1Channel, img2Channel,
				Ch1Name, Ch2Name);
		}

		// create a results handler
		final List<ResultHandler<T>> listOfResultHandlers =
			new ArrayList<>();
		final PDFWriter<T> pdfWriter = new PDFWriter<>(container);
		final SingleWindowDisplay<T> swDisplay = new SingleWindowDisplay<>(
			container, pdfWriter);
		listOfResultHandlers.add(swDisplay);
		listOfResultHandlers.add(pdfWriter);
		if (extraHandlers != null) listOfResultHandlers.addAll(extraHandlers);
		// ResultHandler<T> resultHandler = new EasyDisplay<T>(container);

		// this contains the algorithms that will be run when the user clicks ok
		final List<Algorithm<T>> userSelectedJobs = new ArrayList<>();

		// add some pre-processing jobs:
		userSelectedJobs.add(container.setInputCheck(new InputCheck<T>()));
		userSelectedJobs.add(container.setAutoThreshold(
			new AutoThresholdRegression<>(pearsonsCorrelation,
				AutoThresholdRegression.Implementation.values()[indexRegr])));

		// add user selected algorithms
		addIfValid(pearsonsCorrelation, userSelectedJobs);
		addIfValid(liHistogramCh1, userSelectedJobs);
		addIfValid(liHistogramCh2, userSelectedJobs);
		addIfValid(liICQ, userSelectedJobs);
		addIfValid(SpearmanRankCorrelation, userSelectedJobs);
		addIfValid(mandersCorrelation, userSelectedJobs);
		addIfValid(kendallTau, userSelectedJobs);
		addIfValid(histogram2D, userSelectedJobs);
		addIfValid(costesSignificance, userSelectedJobs);

		// execute all algorithms
		int count = 0;
		final int jobs = userSelectedJobs.size();
		for (final Algorithm<T> a : userSelectedJobs) {
			try {
				count++;
				IJ.showStatus(count + "/" + jobs + ": Running " + a.getName());
				a.execute(container);
			}
			catch (final MissingPreconditionException e) {
				for (final ResultHandler<T> r : listOfResultHandlers) {
					r.handleWarning(new Warning("Probem with input data", a.getName() +
						": " + e.getMessage()));
				}
			}
		}
		// clear status
		IJ.showStatus("");

		// let the algorithms feed their results to the handler
		for (final Algorithm<T> a : userSelectedJobs) {
			for (final ResultHandler<T> r : listOfResultHandlers)
				a.processResults(r);
		}
		// if we have ROIs/masks, add them to results
		if (displayImages) {
			RandomAccessibleInterval<T> channel1, channel2;
			if (mask != null || roi != null) {
				final long[] offset = container.getMaskBBOffset();
				final long[] size = container.getMaskBBSize();
				channel1 = createMaskImage(container.getSourceImage1(), //
					container.getMask(), offset, size);
				channel2 = createMaskImage(container.getSourceImage2(), //
					container.getMask(), offset, size);
			}
			else {
				channel1 = container.getSourceImage1();
				channel2 = container.getSourceImage2();
			}
			channel1 = project(channel1);
			channel2 = project(channel2);

			for (final ResultHandler<T> r : listOfResultHandlers) {
				r.handleImage(channel1, "Channel 1 (Max Projection)");
				r.handleImage(channel2, "Channel 2 (Max Projection)");
			}
		}
		// do the actual results processing
		swDisplay.process();
		// add window to the IJ window manager
		swDisplay.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				WindowManager.removeWindow(swDisplay);
			}
		});
		WindowManager.addWindow(swDisplay);
		// show PDF saving dialog if requested
		if (autoSavePdf) pdfWriter.process();
	}

	private RandomAccessibleInterval<T> project(
		final RandomAccessibleInterval<T> image)
	{
		if (image.numDimensions() < 2) {
			throw new IllegalArgumentException("Dimensionality too small: " + //
				image.numDimensions());
		}

		final IterableInterval<T> input = Views.iterable(image);
		final T type = input.firstElement(); // e.g. unsigned 8-bit
		final long xLen = image.dimension(0);
		final long yLen = image.dimension(1);

		// initialize output image with minimum value of the pixel type
		final long[] outputDims = { xLen, yLen };
		final Img<T> output = new ArrayImgFactory<T>().create(outputDims, type);
		for (final T sample : output) {
			sample.setReal(type.getMinValue());
		}

		// loop over the input image, performing the max projection
		final Cursor<T> inPos = input.localizingCursor();
		final RandomAccess<T> outPos = output.randomAccess();
		while (inPos.hasNext()) {
			final T inPix = inPos.next();
			final long xPos = inPos.getLongPosition(0);
			final long yPos = inPos.getLongPosition(1);
			outPos.setPosition(xPos, 0);
			outPos.setPosition(yPos, 1);
			final T outPix = outPos.get();
			if (outPix.compareTo(inPix) < 0) {
				outPix.set(inPix);
			}
		}
		return output;
	}

	/**
	 * A method to get the bounding box from the data in the given image that is
	 * above zero. Those values are interpreted as a mask. It will return null if
	 * no mask information was found.
	 *
	 * @param mask The image to look for "on" values in
	 * @return a new MaskInfo object or null
	 */
	protected MaskInfo getBoundingBoxOfMask(final Img<T> mask) {
		final Cursor<T> cursor = mask.localizingCursor();

		final int numMaskDims = mask.numDimensions();
		// the "off type" of the mask
		final T offType = mask.firstElement().createVariable();
		offType.setZero();
		// the corners of the bounding box
		long[] min = null;
		long[] max = null;
		// indicates if mask data has been found
		boolean maskFound = false;
		// a container for temporary position information
		final long[] pos = new long[numMaskDims];
		// walk over the mask
		while (cursor.hasNext()) {
			cursor.fwd();
			final T data = cursor.get();
			// test if the current mask data represents on or off
			if (data.compareTo(offType) > 0) {
				// get current position
				cursor.localize(pos);
				if (!maskFound) {
					// we found mask data, first time
					maskFound = true;
					// init min and max with the current position
					min = Arrays.copyOf(pos, numMaskDims);
					max = Arrays.copyOf(pos, numMaskDims);
				}
				else {
					/* Is is at least second hit, compare if it
					 * has new "extreme" positions, i.e. does
					 * is make the BB bigger?
					 */
					for (int d = 0; d < numMaskDims; d++) {
						if (pos[d] < min[d]) {
							// is it smaller than min
							min[d] = pos[d];
						}
						else if (pos[d] > max[d]) {
							// is it larger than max
							max[d] = pos[d];
						}
					}
				}
			}
		}

		if (!maskFound) {
			return null;
		}
		else {
			// calculate size
			final long[] size = new long[numMaskDims];
			for (int d = 0; d < numMaskDims; d++)
				size[d] = max[d] - min[d] + 1;
			// create and add bounding box
			final BoundingBox bb = new BoundingBox(min, size);
			return new MaskInfo(bb, mask);
		}
	}

	/**
	 * Adds the provided Algorithm to the list if it is not null.
	 */
	protected void addIfValid(final Algorithm<T> a,
		final List<Algorithm<T>> list)
	{
		if (a != null) list.add(a);
	}

	/**
	 * Returns true if a custom ROI has been selected, i.e if the current ROI does
	 * not have the extent of the whole image.
	 * 
	 * @return true if custom ROI selected, false otherwise
	 */
	protected boolean hasValidRoi(final ImagePlus imp) {
		final Roi roi = imp.getRoi();
		if (roi == null) return false;

		final Rectangle theROI = roi.getBounds();

		// if the ROI is the same size as the image (default ROI), return false
		return (theROI.height != imp.getHeight() || theROI.width != imp.getWidth());
	}

	/**
	 * Clips a value to the specified bounds.
	 */
	protected static int clip(final int val, final int min, final int max) {
		return Math.max(Math.min(val, max), min);
	}

	/**
	 * This method checks if the given ImagePlus contains any masks or ROIs. If
	 * so, the appropriate date structures are created and filled.
	 */
	protected void createMasksFromImage(final ImagePlus imp) {
		// get ROIs from current image in Fiji
		final Roi[] impRois = split(imp.getRoi());
		// create the ROIs
		createMasksAndRois(impRois, imp.getWidth(), imp.getHeight());
	}

	/**
	 * A method to fill the masks array with data based on the ROI manager.
	 */
	protected boolean createMasksFromRoiManager(final int width,
		final int height)
	{
		final RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			IJ.error("Could not get ROI Manager instance.");
			return false;
		}
		final Roi[] selectedRois = roiManager.getSelectedRoisAsArray();
		// create the ROIs
		createMasksAndRois(selectedRois, width, height);
		return true;
	}

	/**
	 * Creates appropriate data structures from the ROI information
	 * passed. If an irregular ROI is found, it will be put into a
	 * frame of its bounding box size and put into an {@code Image<T>}.
	 *
	 * In the end the members ROIs, masks and maskBBs will be
	 * filled if ROIs or masks were found. They will be null
	 * otherwise.
	 */
	protected void createMasksAndRois(final Roi[] rois, final int width,
		final int height)
	{
		// create empty list
		masks.clear();

		for (final Roi r : rois) {
			final MaskInfo mi = new MaskInfo();
			// add it to the list of masks/ROIs
			masks.add(mi);
			// get the ROIs/masks bounding box
			final Rectangle rect = r.getBounds();
			mi.roi = new BoundingBox(new long[] { rect.x, rect.y }, new long[] {
				rect.width, rect.height });
			final ImageProcessor ipMask = r.getMask();
			// check if we got a regular ROI and return if so
			if (ipMask == null) {
				continue;
			}

			// create a mask processor of the same size as a slice
			final ImageProcessor ipSlice = ipMask.createProcessor(width, height);
			// fill the new slice with black
			ipSlice.setValue(0.0);
			ipSlice.fill();
			// position the mask on the new mask processor
			ipSlice.copyBits(ipMask, (int) mi.roi.offset[0], (int) mi.roi.offset[1],
				Blitter.COPY);
			// create an Image<T> out of it
			final ImagePlus maskImp = new ImagePlus("Mask", ipSlice);
			// and remember it and the masks bounding box
			mi.mask = ImagePlusAdapter.<T> wrap(maskImp);
		}
	}

	/**
	 * This method duplicates the given images, but respects ROIs if present.
	 * Meaning, a sub-picture will be created when source images are
	 * ROI/MaskImages.
	 * 
	 * @throws MissingPreconditionException
	 */
	protected RandomAccessibleInterval<T> createMaskImage(
		final RandomAccessibleInterval<T> image,
		final RandomAccessibleInterval<BitType> mask, final long[] offset,
		final long[] size) throws MissingPreconditionException
	{
		final long[] pos = new long[image.numDimensions()];
		// sanity check
		if (pos.length != offset.length || pos.length != size.length) {
			throw new MissingPreconditionException(
				"Mask offset and size must be of same dimensionality like image.");
		}
		// use twin cursor for only one image
		final TwinCursor<T> cursor = new TwinCursor<>(image.randomAccess(), //
			image.randomAccess(), Views.iterable(mask).localizingCursor());
		// prepare output image
		final ImgFactory<T> maskFactory = new ArrayImgFactory<>();
		// Img<T> maskImage = maskFactory.create( size, name );
		final RandomAccessibleInterval<T> maskImage = maskFactory.create(size, //
			image.randomAccess().get().createVariable());
		final RandomAccess<T> maskCursor = maskImage.randomAccess();
		// go through the visible data and copy it to the output
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			// shift coordinates by offset
			for (int i = 0; i < pos.length; ++i) {
				pos[i] = pos[i] - offset[i];
			}
			// write out to correct position
			maskCursor.setPosition(pos);
			maskCursor.get().set(cursor.getFirst());
		}

		return maskImage;
	}

	/**
	 * Splits a non overlapping composite ROI into its sub ROIs.
	 *
	 * @param roi The ROI to split
	 * @return A list of one or more ROIs
	 */
	public static Roi[] split(final Roi roi) {
		if (roi instanceof ShapeRoi) return ((ShapeRoi) roi).getRois();
		return new Roi[] { roi };
	}

	/**
	 * Main method for easier development. To run this plugin with Maven, use:
	 * 
	 * <pre>
	 * mvn exec:java -Dexec.mainClass="Coloc_2"
	 * </pre>
	 */
	public static void main(final String[] args) {
		Debug.run(null, null);
	}
}
