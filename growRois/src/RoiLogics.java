

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.macro.Interpreter;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.PolygonFiller;

import java.awt.Color;
import java.awt.Frame;
import java.awt.List;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.swing.JScrollPane;

/** 
 * Class with static utility functions for handling Rois and also accessing imageJ's roiManager
 * @author Thomas Braschler, Zahra Sadat Ghazali
 *
 */
public class RoiLogics {

	/** 
	 * Reference to ImageJ's roiManager
	 */
	public static RoiManager roiManager=null;

	/**
	 * Get the count of ROIs at present listed in the ROI manager
	 * @return The count of ROIs
	 */
	public static int getCount()
	{
		if(roiManager==null)
		{
			roiManager=getRoiManager();
		}

		return(roiManager.getCount());
	}

	/**
	 * If a selection is present in the image Plus, add it as ROI to the ROI Manager
	 * @param imp ImagePlus image with possible a selected ROI
	 */

	public static void addSelection(ImagePlus imp)
	{

		if(roiManager==null)
		{
			roiManager=getRoiManager();
		}

		if(imp.getRoi() != null)
		{

			roiManager.addRoi(imp.getRoi());
		}


	}

	/**
	 * Ensures that at least one ROI is available in the RoiManager window
	 * @param imp ImagePlus image from which a roi can be chosen
	 */
	public static void checkRois(ImagePlus imp)
	{
		if(roiManager==null)
		{
			roiManager=getRoiManager();
		}

		if(roiManager.getCount()==0)
		{
			if(imp.getRoi() != null)
			{

				roiManager.addRoi(imp.getRoi());
			}
			else
			{
				int width=Math.round((float)imp.getWidth()/5);
				int height=Math.round((float)imp.getHeight()/5);
				int[] x = new int[4];
				int[] y = new int[4];
				x[0]=width*2;
				y[0]=height*2;
				x[1]=width*3;
				y[1]=height*2;
				x[2]=width*3;
				y[2]=height*3;
				x[3]=width*2;
				y[3]=height*3;

				Roi r = new PolygonRoi(x,y, 4,Roi.POLYGON);
				roiManager.addRoi(r);
			}
		}
	}

	/**
	 * Make a copy of a polygon
	 * @param pol The polygon to be copied
	 * @return Copy of the polygon
	 */

	public static Polygon clonePolygon(Polygon pol)
	{

		Polygon newPol = new Polygon();
		newPol.xpoints = pol.xpoints.clone();
		newPol.ypoints = pol.ypoints.clone();
		newPol.npoints = pol.npoints;
		return newPol;

	}

	/**
	 * Make a copy of an array of polygons
	 * @param pols The array of polygons to be cloned
	 * @return cloned polygon array
	 */

	public static Polygon[] clonePolygonArray(Polygon[] pols)
	{
		if(pols==null)
		{
			return null;
		}
		Polygon[] ret = new Polygon[pols.length];
		for(int index=0; index < pols.length; index++)
		{
			ret[index]=clonePolygon(pols[index]);
		}
		return ret;

	}
	
	/**
	 * Fill the holes in a binary mask
	 * @param ip Image processor holding the masks
	 * @param foreground The forground level (with which to fill the holes)
	 * @param background the background level
	 */

	public static void fill_holes(ImageProcessor ip, int foreground, int background) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		FloodFiller ff = new FloodFiller(ip);
		ip.setColor(127);
		for (int y=0; y<height; y++) {
			if (ip.getPixel(0,y)==background) ff.fill(0, y);
			if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y);
		}
		for (int x=0; x<width; x++){
			if (ip.getPixel(x,0)==background) ff.fill(x, 0);
			if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1);
		}
		byte[] pixels = (byte[])ip.getPixels();
		int n = width*height;
		for (int i=0; i<n; i++) {
			if (pixels[i]==127)
				pixels[i] = (byte)background;
			else
				pixels[i] = (byte)foreground;
		}
	}

	/**
	 * Draw an array of polygons to a binary mask; the interior of the polygons will be filled with the
	 * specified color, the remainder of the surface remains however it was
	 * @param pols The polygons to be drawn
	 * @param bp A byteProcessor, contains the image onto which the polygons are drawn
	 * @param col Color for the interior of the polygons
	 */

	public static void drawPolygonArrayToMask(Polygon[] pols, ByteProcessor bp, Color col)
	{
		if(pols==null)
		{
			return;
		}
		for(int index=0; index<pols.length; index++)
		{
			if(reactangleOverlapsWithImage(pols[index].getBounds(),bp.getWidth(),bp.getHeight()))
			{
				drawPolygonToMask(pols[index],bp,col);
			}
		}
	}

	public static void drawPolygonToMask(Polygon pol, ByteProcessor bp, Color col)
	{

		bp.setColor(col);

		bp.fill(new PolygonRoi(pol, Roi.POLYGON));


	}

	public static Polygon[] getAllButOneElement(Polygon[] pols, int indexToAvoid)
	{
		if(pols==null) { return null; }
		if(pols.length<=1) { return null; }
		int finalIndexToAvoid = indexToAvoid;
		if(indexToAvoid>pols.length)
		{
			finalIndexToAvoid = pols.length;
		}
		if(indexToAvoid<0)
		{
			finalIndexToAvoid = 0;
		}

		Polygon[] ret = new Polygon[pols.length-1];
		int newIndex=0;
		for(int index=0; index<pols.length; index++)
		{
			if(index != finalIndexToAvoid)
			{
				ret[newIndex]=pols[index];
				newIndex++;
			}
		}
		return ret;
	}

	public static Color getBlackColor()
	{
		return new Color(0,0,0);
	}

	public static RoiManager getRoiManager()
	{
		if (roiManager==null) {
			if (Macro.getOptions()!=null && Interpreter.isBatchMode())
				roiManager = Interpreter.getBatchModeRoiManager();
			if (roiManager==null) {
				Frame frame = WindowManager.getFrame("ROI Manager");
				if (frame==null)
					IJ.run("ROI Manager...");
				frame = WindowManager.getFrame("ROI Manager");
				roiManager = (RoiManager)frame;
			}

		}
		return roiManager;
	}

	public static Color getWhiteColor()
	{
		return new Color(255, 255, 255);
	}
	public static Polygon growPolygon(Polygon pol)
	{
		return growPolygon(pol,null);
	}


	/** 
	 * Grows a polygon by pixel, but only into areas that are white on the mask
	 * @param pol The polygon to grow (dilate)
	 * @param maskAllowedPixels The mask defining the allowable growth areas
	 * @return Polygon dilated by one pixel
	 */
	public static Polygon growPolygon(Polygon pol, ImageProcessor maskAllowedPixels)
	{
		return growPolygon(pol,maskAllowedPixels,null);
	}

	/**
	 * Grows a polygon by a pixel, respecting the white areas on the mask and also avoiding the other polygons
	 * @param pol The polygon to be grown (the polygon itself is not changed, the resulting polygon is a new one)
	 * @param maskAllowedPixels 0/255 values to indicate the forbidden and allowed areas
	 * @param polygons_to_avoid An array of polygons to be avoided while growing
	 * @return The polygon grown by a pixel
	 */
	public static Polygon growPolygon(Polygon pol, ImageProcessor maskAllowedPixels, Polygon[] polygons_to_avoid)
	{	
		// In order to save memory, we do the enlargement operation only locally, 
		// so we need to translate the polygon to near the origin



		Rectangle r=pol.getBounds();
		Polygon polCopy = clonePolygon(pol);
		polCopy.translate(-r.x+1, -r.y+1);

		Polygon[] polygons_to_avoid_copy=clonePolygonArray(polygons_to_avoid);

		translatePolygonArray(polygons_to_avoid_copy,-r.x+1,-r.y+1);



		// Use the polygon coordinates to create an image where the pixels inside the polygon are white, the other ones black
		ByteProcessor thePolygonMask = maskFromPolygon(polCopy,r.width+2,r.height+2);
		ByteProcessor allowedMask=new ByteProcessor(r.width+2, r.height+2);

		if(maskAllowedPixels!=null)
		{



			allowedMask.copyBits(maskAllowedPixels, -r.x+1, -r.y+1, Blitter.COPY);



			allowedMask.copyBits(thePolygonMask, 0, 0, Blitter.OR);



			//ip1.copyBits(ip2, 0, 0, mode);

		}
		else
		{
			// all pixels are possible
			allowedMask.add(allowedMask.maxValue());
		}
		ByteProcessor otherPols = null;
		if(polygons_to_avoid_copy != null)
		{
			otherPols = new ByteProcessor(r.width+2, r.height+2);	
			otherPols.setColor(RoiLogics.getWhiteColor());
			otherPols.fill();
			drawPolygonArrayToMask(polygons_to_avoid_copy,otherPols,RoiLogics.getBlackColor());
			drawPolygonToMask(polCopy,otherPols,RoiLogics.getWhiteColor());


		}

		// The binary processing erodes the black, which mean it actually enlargens the polygon
		thePolygonMask.erode();

		if(maskAllowedPixels!=null)
		{
			thePolygonMask.copyBits(allowedMask, 0, 0, Blitter.AND);
		}
		if(otherPols!=null)
		{

			thePolygonMask.copyBits(otherPols, 0, 0, Blitter.AND);



		}





		// Reconstitute a polygon from the mask
		Polygon retPol = polygonFromMask(thePolygonMask);

		// Now, we can shift the polygon back to where it belongs
		retPol.translate(r.x-1, r.y-1);

		return retPol;

	}
	
	/**
	 * Grow polygons
	 * @param pols An array of polygons
	 * @param allowedProcessor The image Processor to use
	 * @param avoidNeighbors Does a growing polygon have to avoid geometric neighbors during growth?
	 * @param nSteps How many pixels to grow?
	 * @param bp Reference to progress bar to show progress
	 */

	public static void growPolygons(Polygon[] pols, ImageProcessor allowedProcessor, boolean avoidNeighbors, int nSteps, ProgressBar bp)
	{
		for(int nindex=0; nindex<nSteps; nindex++)
		{

			boolean showProgress = (bp!=null);	

			if(showProgress)
			{
				bp.show(nindex*pols.length,nSteps*pols.length);
			}

			for(int index=0; index<pols.length; index++)
			{

				Polygon pol = pols[index];

				Polygon[] toAvoid=null;

				if(avoidNeighbors)
				{
					toAvoid = getAllButOneElement(pols,index);

				}

				pols[index] = growPolygon(pol, allowedProcessor,toAvoid);


				if(showProgress & (index % 400) == 0)
				{
					IJ.showProgress(nindex*pols.length+index,nSteps*pols.length);
				}


			}

		}
	}
	
	/**
	 * Grow polygons with progressive enlargements of the mask from watershedding
	 * @param pols An array of polygons
	 * @param allowedProcessor The image Processor to use
	 * @param avoidNeighbors Does a growing polygon have to avoid geometric neighbors during growth?
	 * @param nSteps How many pixels to for each watershed level
	 * @param watershedProcessor Greyscale image containg the watershed guid, first fill low values, then higher
	 * @param bp Reference to progress bar to show progress
	 */
	
	public static void growPolygonsWatershed(Polygon[] pols, ImageProcessor allowedProcessor, boolean avoidNeighbors, int nSteps, ImageProcessor watershedProcessor, ProgressBar bp)
	{
		if(watershedProcessor==null)
		{
			growPolygons(pols, allowedProcessor, avoidNeighbors, nSteps, bp);
			return;
		}
		ByteProcessor w=null;
		if(!(watershedProcessor instanceof ByteProcessor))
		{
			w=watershedProcessor.convertToByteProcessor(true);
		} else
		{
			w=(ByteProcessor)watershedProcessor;
		}
		
		int lower=(int)Math.floor(w.getMin());
		int upper=(int)Math.floor(w.getMax());
		
		for(int theLevel=lower; theLevel<=upper; theLevel++)
		{
			ByteProcessor thresholdMask=maskFromThreshold(w,theLevel);
			thresholdMask.invert();
			if(allowedProcessor!=null)
			{
				thresholdMask.copyBits(allowedProcessor, 0, 0, Blitter.AND);
				drawPolygonArrayToMask(pols, thresholdMask, getWhiteColor());
			}
			
			growPolygons(pols, thresholdMask, avoidNeighbors, nSteps, null);
			boolean showProgress = (bp!=null);	

			if(showProgress)
			{
				bp.show(theLevel-lower,upper-lower);
			}
		}
			
		
		
	}
	
	public static ByteProcessor maskFromThreshold(ImageProcessor ip, int threshold)
	{
		ByteProcessor bp=ip.convertToByteProcessor(false);
		bp.threshold(threshold);
		
		return(bp);
		
	}

	/**
	 * Convert a polygon to a binary mask in rectangular image (white pixels on black background)
	 * @param pol The polygon
	 * @param width Width of the image to be created
	 * @param height Height of the image to be created
	 * @return ByteProcessor containing the mask
	  */
	
	public static ByteProcessor maskFromPolygon(Polygon pol,int width,int height)
	{
		PolygonFiller f = new PolygonFiller(pol.xpoints,pol.ypoints, pol.npoints);
		ByteProcessor theMask = (ByteProcessor)f.getMask(width, height);

		theMask.setColor(RoiLogics.getWhiteColor());
		Rectangle r2=new Rectangle();
		r2.x=0;
		r2.y=0;
		r2.width=width;
		r2.height=height;
		f.fill(theMask, r2);
		return theMask;

	}

	/**
	 * Convert a binary mask to a polygon
	 * @param theMask ByteProcessor containing the mask (white pixels = selected)
	 * @return Polygon obtained from the mask
	  */

	public static Polygon polygonFromMask(ByteProcessor theMask)
	{
		// We need a black rim, the algorithm does not necessarily work if the pixels touch borders



		Wand thewand = new Wand(theMask);
		Wand.setAllPoints(true);

		// We need to find a starting point, let's try to find a white one near the center of gravity

		double cgx=0;
		double cgy=0;
		double cgn=0;
		boolean found=false;
		for(int xs=0; xs<theMask.getWidth(); xs++)
		{
			for(int ys=0; ys<theMask.getHeight(); ys++)
			{
				if((theMask.getPixel(xs, ys)>128))
				{
					found=true;
					cgx=cgx+xs;
					cgy=cgy+ys;
					cgn++;
				}
			}
		}

		if(!found)
		{
			return null;
		}


		cgx=cgx/cgn;
		cgy=cgy/cgn;



		double dsquared=2*(theMask.getWidth()*theMask.getWidth()+theMask.getHeight()*theMask.getHeight());

		found=false;
		int xstart=-1;
		int ystart=-1;
		for(int xs=0; xs<theMask.getWidth(); xs++)
		{
			for(int ys=0; ys<theMask.getHeight(); ys++)
			{
				if((theMask.getPixel(xs, ys)>128) & ((xs-cgx)*(xs-cgx)+(ys-cgy)*(ys-cgy)<dsquared))
				{
					found=true;
					xstart=xs;
					ystart=ys;
					dsquared = (xs-cgx)*(xs-cgx)+(ys-cgy)*(ys-cgy);
				}
			}
		}

		if(!found)
		{
			return null;
		}








		thewand.autoOutline(xstart, ystart);

		Polygon retPol = new Polygon();

		retPol.xpoints = thewand.xpoints.clone();
		retPol.ypoints = thewand.ypoints.clone();
		retPol.npoints = thewand.npoints;





		return(retPol);

	}
	
	/**
	 * Does a given rectangle overlap at least partially with an image of given width and height?
	 * @param r The rectangle for testing overlap
	 * @param width The width of the image (image goes from 0 to width, interval edges included)
	 * @param height The height of the image (image goes from 0 to width, interval edges included) 
	 * @return Whether or not there is overlap
	  */
	

	public static boolean reactangleOverlapsWithImage(Rectangle r,int width, int height)
	{
		return ((r.x<=width) & (r.x+r.width>=0) & (r.y<=height) & (r.y+r.height>=0));

	}
	
	/**
	 * Specifically redraws the scroll-Pane part of the Roimanager (where the list of Rois is)
  */

	public static void redrawScrollPane()
	{
		RoiManager r=getRoiManager();

		JScrollPane js = (JScrollPane)r.getComponent(0);

		js.setVisible(false);
		js.setVisible(true);
	}
	
	/**
	 * Helper function, renames ROI in ROI manager
	 * @param index The index of the ROI to rename
	 * @param label The new name
    */

	@SuppressWarnings("unchecked")
	public static void renameLabelInRoiManager(int index, String label)
	{
		List l = roiManager.getList();
		String oldLabel = l.getItem(index);
		Roi theRoi=(Roi) roiManager.getRoisAsArray()[index];
		if(theRoi==null)
		{
			IJ.error("Internal error with ROI "+oldLabel);
			return;
		}
		theRoi.setName(label);
		roiManager.getROIs().remove(oldLabel);
		roiManager.getROIs().put(label, theRoi);
		l.remove(index);
		l.add(label,index);
	}

	/**
	 * Replace a ROI in the list with a new one
	 * @param index The index of the ROI to replace
	 * @param newRoi The new ROI to put
    */
	@SuppressWarnings("unchecked")
	public static void replaceRoiinRoiManager(int index, Roi newRoi)
	{
		String oldLabel = RoiManager.getName(""+index);
		if(newRoi != null)
		{
			newRoi.setName(oldLabel);
		}
		roiManager.getROIs().remove(oldLabel);
		if(newRoi != null)
		{
			roiManager.getROIs().put(oldLabel, newRoi);
		}
	}

	/**
	 * Translate an array of polygons, all be the same delta x and y
	 * @param pols The polygons to be translated
	 * @param deltaX The delta in X position
	 * @param deltaY the delta in Y position
    */
	public static void translatePolygonArray(Polygon[] pols,int deltaX, int deltaY)
	{
		if(pols==null){ return; }
		for(int index=0; index<pols.length; index++)
		{
			pols[index].translate(deltaX, deltaY);
		}

	}
	

	/**
	 * Debugging function, used to show some image processor in ImageJ to see its content
	 * @param processor The image processor to be shown
    */
	
	public static void testShow(ImageProcessor processor)
	{
		ImagePlus gg=new ImagePlus("Test",processor);
		gg.show();

	}
	
	/**
	 * Convert a mask to a ROI
	 * @param ip Image processor holding the mask
	 * @param invert Should it be inverted
	 * @return The Roi
    */

	public static Roi getRoiFromMask(ImageProcessor ip, boolean invert)
	{

		ByteProcessor bp = new ByteProcessor(ip.getWidth(),ip.getHeight());
		bp.copyBits(ip, 0, 0, Blitter.COPY);
		ImagePlus ipp=new ImagePlus("gg");
		ipp.setProcessor(bp);


		ThresholdToSelection ts = new ThresholdToSelection();
		ts.setup("", ipp);
		if(invert)
		{
			bp.setThreshold(0, 128, ImageProcessor.NO_LUT_UPDATE);
		}
		else
		{
			bp.setThreshold(128, 255, ImageProcessor.NO_LUT_UPDATE);
		}
		ts.run(bp);
		Roi r = ipp.getRoi();

		return r;

	}
	
	/**
	 * Fill a ROI with foreground color (white)
	 * @param theRoi the ROI (region of interest)
	 * @param bp Image processor in which the drawing should be carried out
    */

	public static void drawMaskFromRoi(Roi theRoi, ByteProcessor bp)
	{
		bp.setColor(getWhiteColor());
		bp.fill(theRoi);


	}
	
	/**
	 * Translate a ROI by delta x and delta y
	 * @param theRoi The region of interest
	 * @param deltax The delta in X position
	 * @param deltay the delta in Y position
    */

	public static void translateRoi(Roi theRoi,int deltax, int deltay)
	{
		if(theRoi==null)
		{
			return;
		}

		Rectangle bounds=theRoi.getBounds();
		theRoi.setLocation(bounds.x+deltax, bounds.y+deltay);


	}
	
	/** Helper function for making a string from an int
	 * 
	 * @param num The integer number
	 * @param digits Minimum number of digits (for leading zeroes)
	 * @return string formatted number
	 */

	public static String intToString(int num, int digits)
	{
		String format = String.format("%%0%dd", digits);
		String result = String.format(format, num);
		return result;
	}
	
	/** Helper function for making a string from an int
	 * 
	 * @param num The integer number
	 * @param maximum Highest number to be shown, to define the number of necessary leading zeroes
	 * @return string-formatted number
	 */
	
	public static String intWithLeadingZeroes(int num, int maximum)
	{
		int digits = (int)Math.ceil(Math.log((double)maximum+1)/Math.log(10));
		return intToString(num,digits);
	}
	
	/** Keep only the parts of a ROI that are within the area defined by a mask
	 * 
	 * @param theRoi The ROI to be trimmed to the mask area
	 * @param ip The image processor holding the mask
	 * @return the trimmed ROI
	 */

	public static Roi restrictRoiToMask(Roi theRoi,ImageProcessor ip)
	{
		if(theRoi==null)
		{
			return null;
		}
		Rectangle bounds = theRoi.getBounds();

		translateRoi(theRoi,-bounds.x,-bounds.y);


		ByteProcessor bp = new ByteProcessor(bounds.width,bounds.height);
		drawMaskFromRoi(theRoi,bp);

		bp.copyBits(ip, -bounds.x, -bounds.y, Blitter.AND);



		Roi r= getRoiFromMask(bp,false);
		translateRoi(theRoi, bounds.x, bounds.y);

		translateRoi(r,bounds.x,bounds.y);

		if(r==null)
		{ return null; }
		r.setName(theRoi.getName());
		return r; 
	}

}
