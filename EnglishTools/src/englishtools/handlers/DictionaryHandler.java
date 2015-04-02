package englishtools.handlers;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.texteditor.ITextEditor;

public class DictionaryHandler extends AbstractHandler {
    static final String DICTIONARY_URL = "http://dictionary.reference.com/browse/";
    
    public DictionaryHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
	    try {
    	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if(part instanceof ITextEditor){
                ITextEditor editor = (ITextEditor)part;
                ISelection sel = editor.getSelectionProvider().getSelection();
                if ( sel instanceof TextSelection ) {
                    final TextSelection textSel = (TextSelection)sel;
                    final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(null);
                    browser.openURL(new URL(DICTIONARY_URL+textSel.getText()));                
                }
            } 
        } catch (IOException | PartInitException e) {
            e.printStackTrace();
        }
		return null;
	}
}
