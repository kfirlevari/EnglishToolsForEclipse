package englishtools.handlers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class ThesaurusHandler extends AbstractHandler {
    static final String THEASAURUS_URL = "http://www.thesaurus.com/browse/";
    
    public static class SynonymProperties implements Comparable<SynonymProperties>{
        
        public SynonymProperties(String word, int relevance) {
            this.word = word;
            this.relevance = relevance;
        }
        
        String word;
        int relevance;
        
        @Override
        public int compareTo(SynonymProperties o) {
            return o.relevance - this.relevance ;
        }
    }
    
    public ThesaurusHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    if(part instanceof ITextEditor){
	        ITextEditor editor = (ITextEditor)part;
	        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
	        ISelection sel = editor.getSelectionProvider().getSelection();
	        if ( sel instanceof TextSelection ) {
	            openThesaurusDialog(event, document, sel);
	        }
	    }
		return null;
	}

    private void openThesaurusDialog(ExecutionEvent event, IDocument document,
            ISelection sel) throws ExecutionException {
        String selectedWord;
        final TextSelection textSel = (TextSelection)sel;
        selectedWord = textSel.getText();
        List<SynonymProperties> result = listOfSynonyms(selectedWord);
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        ThesaurusSelectionDialog t = new ThesaurusSelectionDialog(
                window.getShell(),
                selectedWord,
                result,
                document,
                textSel);
        t.open();
    }

    private List<SynonymProperties> listOfSynonyms(String selectedWord) {
        LinkedList<SynonymProperties> res = new LinkedList<SynonymProperties>();
        try {
            Document doc = Jsoup.connect(THEASAURUS_URL+selectedWord).get();
            Elements synonyms = doc.select(".relevancy-list");
            for (Element a : synonyms.get(0).select("a")){
                res.add(new SynonymProperties(
                        ((TextNode)a.getElementsByClass("text").get(0).childNode(0)).text(),
                        Integer.valueOf(a.attr("data-category").replace("{\"name\": \"relevant-", "").substring(0,1))));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort(res);
        return res;
    }
    
    public class ThesaurusSelectionDialog extends Dialog {

        final String sourceWord;
        final List<SynonymProperties> synonyms;
        final IDocument doc;
        final TextSelection textSelection;
        
        public ThesaurusSelectionDialog(Shell parentShell, String word, List<SynonymProperties> result, IDocument document, TextSelection textSel) {
          super(parentShell);
          this.sourceWord = word;
          this.synonyms =result;
          this.doc = document;
          this.textSelection = textSel;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            addSynonymsButtons(parent, composite);
            addWebButtons(parent, composite);
            return parent;
        }

        private void addSynonymsButtons(Composite parent, Composite composite) {
            int numOfCol = (synonyms.size() < 40) ? (synonyms.size() / 5) + 1 : 8;
            composite.setLayout(new GridLayout(numOfCol, true));
            for (SynonymProperties s : synonyms){
                final String newWord = s.word;
                Button button = new Button(composite, SWT.PUSH);
                button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,false));
                button.setText(newWord);
                button.setBackground(relevanceToColor(parent.getDisplay(),s.relevance));
                button.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            try {
                                doc.replace( textSelection.getOffset(), textSelection.getLength(), newWord);
                                composite.getShell().close();
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                });
            }
        }

        private void addWebButtons(Composite parent, Composite composite) {
            Button button = new Button(composite, SWT.PUSH);
            button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,false));
            button.setText("Got to Thesaurus.com in Eclipse");
            button.setBackground(new Color(parent.getDisplay(), 255, 128, 0));
            button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        try {
                            final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(null);
                            browser.openURL(new URL(THEASAURUS_URL+sourceWord));
                        } catch (PartInitException | MalformedURLException e1) {
                            e1.printStackTrace();
                        }
                        composite.getShell().close();
                    }
            });
            button.setFocus();
            button = new Button(composite, SWT.PUSH);
            button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,false));
            button.setText("Go to Thesaurus.com in browser");
            button.setBackground(new Color(parent.getDisplay(), 255, 128, 0));
            button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        try {
                            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(THEASAURUS_URL+sourceWord));
                        } catch (PartInitException | MalformedURLException e1) {
                            e1.printStackTrace();
                        }
                        composite.getShell().close();
                    }
            });
            button.setFocus();
        }

        protected Color relevanceToColor(Display d, int relevance){
            switch (relevance){
            case 3:
                return new Color(d, 252, 187, 69);
            case 2:
                return new Color(d,251, 212, 142);
            default:
                return new Color(d, 252, 232, 196);
            }
        }
        
        @Override
        protected void configureShell(Shell newShell) {
          super.configureShell(newShell);
          newShell.setText("Thesaurus results for \""+ sourceWord + "\"");
        }
        
        protected void createButtonsForButtonBar(Composite parent) { 
        } 
        
        protected boolean isResizable() {
            return true;
        }

      }
}
