package de.schneidertim.eclipse_iso_tools.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.github.stephenc.javaisotools.iso9660.ConfigException;
import com.github.stephenc.javaisotools.iso9660.ISO9660RootDirectory;
import com.github.stephenc.javaisotools.iso9660.impl.CreateISO;
import com.github.stephenc.javaisotools.iso9660.impl.ISO9660Config;
import com.github.stephenc.javaisotools.iso9660.impl.ISOImageFileHandler;
import com.github.stephenc.javaisotools.joliet.impl.JolietConfig;
import com.github.stephenc.javaisotools.rockridge.impl.RockRidgeConfig;
import com.github.stephenc.javaisotools.sabre.HandlerException;
import com.github.stephenc.javaisotools.sabre.StreamHandler;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CreateISOHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		try {
			//select root-folder of the new *.iso image:
			File selectedFileOrFolder = getSelectedFileOrFolder(getSelection());
			//let the user select where the resulting ISO File is stored:
			File isoFile = selectISOFile(window.getShell());
			if (isoFile==null) {
				return null;
			}
			
			//volumeID = ISO's filename without '.iso' file-extension:
			String isoName = isoFile.getName();
			String volumeID = isoName.substring(0, isoName.length()-".iso".length()-1);
			//create ISO from SelectedFileOrFolder:
			createIsoForFileOrFolder(selectedFileOrFolder, isoFile,volumeID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	protected void createIsoForFileOrFolder(File contents, File outfile, String volumeID){
		try {
			ISO9660RootDirectory root = new ISO9660RootDirectory();
	        StreamHandler streamHandler;
			root.addContentsRecursively(contents);
			streamHandler = new ISOImageFileHandler(outfile);
			CreateISO iso = new CreateISO(streamHandler, root);
	        ISO9660Config iso9660Config = new ISO9660Config();
	        iso9660Config.allowASCII(false);
	        iso9660Config.setInterchangeLevel(2);//<-File names up to 31 characters
	        iso9660Config.restrictDirDepthTo8(true);
	        iso9660Config.setVolumeID(volumeID);
	        iso9660Config.forceDotDelimiter(true);
	        RockRidgeConfig rrConfig = new RockRidgeConfig();
	        rrConfig.setMkisofsCompatibility(true);
	        rrConfig.hideMovedDirectoriesStore(true);
	        rrConfig.forcePortableFilenameCharacterSet(true);

	        JolietConfig jolietConfig = new JolietConfig();
	        jolietConfig.setVolumeID(volumeID);
	        jolietConfig.forceDotDelimiter(true);

	        iso.process(iso9660Config, rrConfig, jolietConfig, null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
	}
	
	protected ISelection getSelection(){
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = 	window.getSelectionService().getSelection(ProjectExplorer.VIEW_ID);
		return selection;
	}
	
	protected File getSelectedFileOrFolder(ISelection selection){
		if (selection instanceof IStructuredSelection) {
		    IStructuredSelection ssel = (IStructuredSelection) selection;
		    Object obj = ssel.getFirstElement();
		    
		    //# Try adapt as IFile:
		    IFile ifile = (IFile) Platform.getAdapterManager().getAdapter(obj, IFile.class);		    
		    
		    
		    if (ifile == null) {
		        if (obj instanceof IAdaptable) {
		            ifile = (IFile) ((IAdaptable) obj).getAdapter(IFile.class);
		        }
		    }

		    if (ifile != null) {
		    	IPath path = ifile.getFullPath();
		    	return path.toFile();
		    }
		    
		    //# Try adapt as IResource:
		    IResource res = Platform.getAdapterManager().getAdapter(obj, IResource.class);
		    if (res == null) {
		    	if (obj instanceof IAdaptable) {
		            res = (IResource) ((IAdaptable) obj).getAdapter(IResource.class);
		        }
			}
		    
		    if (res != null) {
		    	return new File(res.getLocationURI());
		    }
		}
		throw new RuntimeException("No proper file or resource was selected!");
	}

	protected File selectISOFile(Shell parent){
		FileDialog dialog = new FileDialog(parent, SWT.SAVE);
		dialog.setFilterExtensions(new String[]{"*.iso"});
		String filename=dialog.open();
		if (filename==null) {
			return null;
		}
		return new File(filename);
	}

	protected String createVolumeID(String intendedVolumeID){
		//VolumeIDs may not be longer than 8 Characters
		if (intendedVolumeID.length()>31) {
			return intendedVolumeID.substring(0, 30);
		}
		return intendedVolumeID;
	}
}
