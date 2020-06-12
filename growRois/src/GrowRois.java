import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Polygon;

import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;


/** 
 * This ImageJ plugin allows to grow (dilate) ROIs (regions of interest)
 * simultaneousy, optionally with additional control options 
 * Namely, avoiding overlap between neighboring ROIs and restricting the growth area to a preset mask
 * The primary intended use is cell segmentation by using the nuclei as the starting point; in this case, 
 * pancytoplasmic marking allows to delimit the cells, while avoid overlap leads to a modified watershed that
 * centers the cells on the nuclei. 
 * @author Thomas Braschler, Zahra Sadat Ghazali
 *
 */
public class GrowRois implements PlugInFilter {

	/** Reference to ImageJ's roiManager */
	
	protected RoiManager roiManager;

	/** Current image */
	protected ImagePlus imp;
	
    /** Current image processor */
	protected ImageProcessor ip;
    
	/** Optional mask specifying the accessible pixels for ROI dilatation */
	public ImagePlus allowedPixelMask=null;

	/** Title of the optional mask */
	public static String allowedMaskTitle=null;

	/** Flag indicating whether neighboring ROIs are allowed to overlap */
	public static boolean overlapAllowed=false;

	/** Total number of pixels by which the dilatation should be done */
	public static int nPixels=1;
    
	/** Array of the ROIs to be grown */
	public Roi[] theRois;
    
	/** Did the user push the cancel button in the options dialog */
	protected static boolean iscanceled = false;

	/** 
	 * Main function called by imageJ to have the plugin run 
	 * @param ip Imageprocessor provided by ImageJ
	 * */
	public void run(ImageProcessor ip) {

		
		
		iscanceled = false;

		this.ip = ip;
		
		roiManager=RoiLogics.getRoiManager();

		if(RoiLogics.getCount()==0)
		{
			RoiLogics.addSelection(imp);
		}
		
		if(RoiLogics.getCount()==0)
		{
			IJ.error("growRois: At least one ROI must be available in the ROI manager \n"
					+ "(Analyze > Tools > ROI Manager)");
			return;
		}
		
		

		runDialog();

		if(iscanceled)
		{
			return;
		}	

		
		
		Roi[] theRois = roiManager.getRoisAsArray();
		
		WindowManager.setWindow(WindowManager.getWindow(roiManager.getTitle()));
		
		
		// Rename the rois to avoid issues with automatic renaming upon change of position
		String prefix = "GR_";
		int minimumDigits = 3;
		
		for(int index=0; index<theRois.length; index++)
		{
			String label = prefix+RoiLogics.intToString(index, minimumDigits);
			RoiLogics.renameLabelInRoiManager(index, label);
			
		}
		
		

		theRois =  roiManager.getRoisAsArray();

		IJ.showMessage("Analyzing "+theRois.length+" Rois");

		Polygon[] pols = new Polygon[theRois.length];
		String[] labels = new String[theRois.length];
		for(int index=0; index<theRois.length; index++)
		{
			pols[index] = theRois[index].getPolygon();
			labels[index]=theRois[index].getName();
		}

		ImageProcessor allowedProcessor = null;
		if(allowedPixelMask!=null)
		{
			allowedProcessor = allowedPixelMask.getProcessor();
		}

		ProgressBar bp = new ProgressBar(0, 0);
		ip.setProgressBar(bp);

		

		RoiLogics.growPolygons(pols, allowedProcessor, !overlapAllowed, nPixels, bp);
		
		
		// At present, this seems to be a difficult problem: Changing a given
		// ROI in the roiManager does not seem easy by programmatic access
		// instead, we delete the whole list and add everything new

		roiManager.runCommand("deselect");
		roiManager.runCommand("delete");



		for(int index=0; index<pols.length; index++)
		{


			PolygonRoi n = new PolygonRoi(pols[index], Roi.POLYGON);

			roiManager.add(imp, n, -1);


		}
		
		// This is a bit of fiddling, but it is convenient to have the old names back
		bp.show(1);
		for(int index=0; index<pols.length; index++)
		{
			if(!labels[index].startsWith("GR_"))
			{
				RoiLogics.renameLabelInRoiManager(index,"GR_"+labels[index]);
			}
			else
			{
				RoiLogics.renameLabelInRoiManager(index,labels[index]);
			}
		}
		// Bug with RoiManager, sometimes the ROIs become invisible. Hopefully this helps
		RoiLogics.redrawScrollPane();


	}

	






    /**
     * This function gathers the IDs of the open images. This is to offer the user the choice among the
     * open images as dilatation masks
     * @return Array of open image IDs
     */
	public int[] getImageIDList()
	{
		int[] wList = WindowManager.getIDList();
		int validWindows=0;
		for(int index=0; index<wList.length; index++)
		{
			ImagePlus impro = WindowManager.getImage(wList[index]);
			if(impro!=null)
			{

				validWindows++;
			}

		}
		int[] ret = new int[validWindows];
		int retindex=0;
		for(int index=0; index<wList.length; index++)
		{
			ImagePlus impro = WindowManager.getImage(wList[index]);
			if(impro!=null)
			{
				ret[retindex]=wList[index];
				retindex++;
			}

		}
		return ret;

	}
	/**
	 * Run the options dialog
	 */

	public void runDialog()
	{
		int[] wList = getImageIDList();
		int[] wListWithNone = new int[wList.length+1];
		wListWithNone[0]=0;
		for (int i=0; i<wList.length; i++) {
			wListWithNone[i+1]=wList[i];
		}
		String[] titles = new String[wListWithNone.length];
		titles[0]="<None>";
		for (int i=0; i<wList.length; i++) {
			titles[i+1]=WindowManager.getImage(wList[i]).getTitle();
		}

		IJ.register(GrowRois.class);


		GenericDialog gd = new GenericDialog("Image Calculator", IJ.getInstance());
		String defaultImageTitle=null;
		if(allowedMaskTitle!=null)
		{
			defaultImageTitle=allowedMaskTitle;
		}
		else
		{
			defaultImageTitle=titles[0];
		}
		gd.addChoice("Image with allowed pixels:", titles,defaultImageTitle);
		gd.addCheckbox("Allow overlap between ROIs", overlapAllowed);
		gd.addNumericField("Pixels to grow", nPixels, 0);
		gd.showDialog();
		if (gd.wasCanceled())
		{
			iscanceled =true;
			return;
		}
		int index1 = gd.getNextChoiceIndex();
		allowedMaskTitle = titles[index1];
		if(index1>0)
		{
			allowedPixelMask=WindowManager.getImage(wListWithNone[index1]);
		}
		overlapAllowed = gd.getNextBoolean();

		nPixels=(int)gd.getNextNumber();
		if(nPixels<1) { nPixels=1; }


	}
	
	/** 
	 * Get a reference to ImageJ's roiManager, if not present initiate
	 */

	public void initiateRoiManager()
	{
		roiManager=RoiLogics.getRoiManager();
	}
	
	/** 
	 * Debugging function, initiate some ROI for testing purposes 
	 * */

	public void checkRois()
	{
		if(roiManager==null)
		{
			initiateRoiManager();
		}

		RoiLogics.checkRois(imp);

	}





	/**
	 * Setup function used by ImageJ to know the basic properties of the plugin
	 */

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

}
