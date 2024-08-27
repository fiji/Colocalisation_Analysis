/*
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2024 Fiji developers.
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
package sc.fiji.coloc;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ColocImgLibGadgets<T extends RealType<T> & NativeType<T>> implements PlugIn {

  protected Img<T> img1, img2;

  @Override
  public void run(String arg) {
	ImagePlus imp1 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/red.tif");
	img1 = ImagePlusAdapter.wrap(imp1);
	ImagePlus imp2 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/green.tif");
	img2 = ImagePlusAdapter.wrap(imp2);

	double pearson = calculatePearson();

	Img<T> ranImg = generateRandomImageStack(img1, new int[] {2,2,1});
  }

  /**
   * To randomize blockwise we enumerate the blocks, shuffle that list and
   * write the data to their new position based on the shuffled list.
   */
  protected Img<T> generateRandomImageStack(Img<T> img, int[] blockDimensions) {
	int numberOfDimensions = Math.min(img.numDimensions(), blockDimensions.length);
	int numberOfBlocks = 0;
	long[] numberOfBlocksPerDimension = new long[numberOfDimensions];

	for (int i = 0 ; i<numberOfDimensions; i++){
		if (img.dimension(i) % blockDimensions[i] != 0){
			System.out.println("sorry, for now image dims must be divisable by block size");
			return null;
		}
		numberOfBlocksPerDimension[i] = img.dimension(i) / blockDimensions[i];
		numberOfBlocks *= numberOfBlocksPerDimension[i];
	}
	List<Integer> allTheBlocks = new ArrayList<Integer>(numberOfBlocks);
	for (int i = 0; i<numberOfBlocks; i++){
		allTheBlocks.add(new Integer(i));
	}
	Collections.shuffle(allTheBlocks, new Random());
	Cursor<T> cursor = img.cursor();

	// create factories for new image stack
	//ContainerFactory containerFactory = new ImagePlusContainerFactory();
	ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
	//new ImageFactory<T>(cursor.getType(), containerFactory);

	// create a new stack for the random images
	final long[] dim = new long[ img.numDimensions() ];
	img.dimensions(dim);
	Img<T> randomStack = imgFactory.create(dim, img.firstElement().createVariable());

	// iterate over image data
	while (cursor.hasNext()) {
		cursor.fwd();
		T type = cursor.get();
		// type.getRealDouble();
	}

	return randomStack;
  }

  protected double calculatePearson() {
	Cursor<T> cursor1 = img1.cursor();
	Cursor<T> cursor2 = img2.cursor();

	double mean1 = getImageMean(img1);
	double mean2 = getImageMean(img2);

	// Do some rather simple performance testing
	long startTime = System.currentTimeMillis();

	double pearson = calculatePearson(cursor1, mean1, cursor2, mean2);

	// End performance testing
	long finishTime = System.currentTimeMillis();
	long elapsed = finishTime - startTime;

	// print some output to IJ log
	IJ.log("mean of ch1: " + mean1 + " " + "mean of ch2: " + mean2);
	IJ.log("Pearson's Coefficient " + pearson);
	IJ.log("That took: " + elapsed + " ms");

	return pearson;
  }

  protected double calculatePearson(Cursor<T> cursor1, double mean1, Cursor<T> cursor2, double mean2) {
	double pearsonDenominator = 0;
	double ch1diffSquaredSum = 0;
	double ch2diffSquaredSum = 0;
	while (cursor1.hasNext() && cursor2.hasNext()) {
		cursor1.fwd();
		cursor2.fwd();
		T type1 = cursor1.get();
		double ch1diff = type1.getRealDouble() - mean1;
		T type2 = cursor2.get();
		double ch2diff = type2.getRealDouble() - mean2;
		pearsonDenominator += ch1diff*ch2diff;
		ch1diffSquaredSum += (ch1diff*ch1diff);
		ch2diffSquaredSum += (ch2diff*ch2diff);
	}
	double pearsonNumerator = Math.sqrt(ch1diffSquaredSum * ch2diffSquaredSum);
	return pearsonDenominator / pearsonNumerator;
  }

  protected double getImageMean(Img<T> img) {
	  double sum = 0;
	  Cursor<T> cursor = img.cursor();
	  while (cursor.hasNext()) {
		  cursor.fwd();
		  T type = cursor.get();
		  sum += type.getRealDouble();
	  }
	  return sum / ImageStatistics.getNumPixels(img);
  }
}
