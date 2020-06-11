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

import java.awt.Polygon;


public class growRois_ implements PlugInFilter {

	protected RoiManager roiManager;

	protected ImagePlus imp;

	protected ImageProcessor ip;

	public ImagePlus allowedPixelMask=null;

	public static String allowedMaskTitle=null;

	public static boolean overlapAllowed=false;

	public static int nPixels=1;

	Roi[] theRois;

	protected static boolean iscanceled = false;

	public void run(ImageProcessor ip) {

		iscanceled = false;

		this.ip = ip;

		checkRois();

		runDialog();

		if(iscanceled)
		{
			return;
		}	

		if(roiManager.getRoisAsArray().length==0)
		{
			IJ.error("growRois: At least one ROI must be available in the ROI manager");
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
		
		
		

		roiManager.runCommand("deselect");
		roiManager.runCommand("delete");



		for(int index=0; index<pols.length; index++)
		{


			PolygonRoi n = new PolygonRoi(pols[index], Roi.POLYGON);

			roiManager.add(imp, n, -1);


		}

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
		
		


	}

	







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

		IJ.register(growRois_.class);


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

	public void initiateRoiManager()
	{
		roiManager=RoiLogics.getRoiManager();
	}



	public void checkRois()
	{
		if(roiManager==null)
		{
			initiateRoiManager();
		}

		RoiLogics.checkRois(imp);

	}







	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

}
