package algorithms;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImageStatistics;

/**
 * This algorithm calculates Percent Overlap
 * in terms of pixels and intensity above threshold 
 * for both of the two channels,
 * from the point of view of each channel with respect to the other.
 * 
 * @author Dan White, GE Healthcare
 * 
 * Here follows Notes from section 6.3 of 
 * http://www.uhnresearch.ca/facilities/wcif/imagej/colour_analysis.htm
 * which define the output of the depreciated Colocalization Threshold plugin 
 * which we reimplement here
 * 
 *  Number of colocalised voxels – Ncoloc
 *  This is the number of voxels which have both channel 1 and channel 2 
 *  intensities above threshold (i.e., the number of pixels in the yellow area 
 *  of the scatterplot).
 *  
 *  %Image volume colocalised – %Volume 
 *  This is the percentage of voxels which have both 
 *  channel 1 and channel 2 intensities above threshold,
 *  expressed as a percentage of the total number of
 *  pixels in the image (including zero-zero pixels);
 *  in other words, the number of pixels in the
 *  scatterplot’s yellow area ÷  total number of pixels in the scatter plot
 *  (the Red + Green + Blue + Yellow areas). 
 *  
 *  %Voxels Colocalised – %Ch1 Vol; %Ch2 Vol 
 *  This generates a value for each channel.
 *  This is the number of voxels for each channel which have both
 *  channel 1 and channel 2 intensities above threshold,
 *  expressed as a percentage of the total number of voxels for
 *  each channel above their respective thresholds; in other words,
 *  for channel 1 (along the x-axis), this equals the
 *  (the number of pixels in the Yellow area) ÷ (the number of pixels in the Blue + Yellow areas).
 *  For channel 2 this is calculated as follows:
 *  (the number of pixels in the Yellow area) ÷ (the number of pixels in the Red + Yellow areas). 
 *  
 *  %Intensity Colocalised – %Ch1 Int; %Ch2 Int
 *  This generates a value for each channel.
 *  For channel 1, this value is equal to the sum of the pixel intensities,
 *  with intensities above both channel 1 and channel 2 thresholds expressed as
 *  a percentage of the sum of all channel 1 intensities;
 *  in other words, it is calculated as follows:
 *  (the sum of channel 1 pixel intensities in the Yellow area) ÷ (the sum of channel 1 pixels intensities in the Red + Green + Blue + Yellow areas).
 *  
 *  %Intensities above threshold colocalised – %Ch1 Int > thresh; %Ch2 Int > thresh
 *  This generates a value for each channel.
 *  For channel 1, this value is equal to the sum of the
 *  pixel intensities with intensities above both channel 1 and channel 2 thresholds
 *  expressed as a  percentage of the sum of all channel 1 intensities above the
 *  threshold for channel 1.
 *  In other words, it is calculated as follows:
 *  (the sum of channel 1 pixel intensities in the Yellow area) ÷ (sum of channel 1 pixels intensities in the Blue + Yellow area)
 *
 */
public class PercentOverlapThresholdedPixelsAndIntensity extends Algorithm<T> {

/**
 * TODO: - implement it all	;-)
 * 
 * Must have a check to see if the is a Mask, if not refuse to run, and thow an error. 
 * Not just a warning, rather a refusal to run 
 * Running these measures without a ROI will nearly always generate nonsesne as the amount of foreground
 * always varies... and even when all the images should be analysed, its easy enough to be explicit 
 * amout thats bing the case by setting the mask to by all pixels. 
 * 
 * 4 methods, all talking a mask, as these measures only make sense with a region of interest,
 * % intensity  and %pixels above thresholds from perspective of either channel or the other. 
 * 
 * 1 method to get number of colcoalized pixels in total.
 * total no of pixels in the ROI (or rather the mask) is alrealdy calculated in ImageStatistics
 * and the DataContainer now grabs that number as a long, so we can use it here. 
 */
	
	
}
