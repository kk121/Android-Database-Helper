import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.oracle.tools.packager.Log;
import org.apache.http.util.TextUtils;

/**
 * Created by krishna on 30/09/17.
 */
public class SqliteModelCreatorAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        // TODO: insert action logic here
        Project project = event.getData(PlatformDataKeys.PROJECT);
        FileEditor fileEditor = event.getData(PlatformDataKeys.FILE_EDITOR);
        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
//        String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
        String projectInfo = project.getName() + "\n" + project.getBaseDir().getPath() + "\n" + project.getBaseDir().getPath();
//        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.\n" + projectInfo, "Information", Messages.getInformationIcon());
        System.out.print("before dialog");
        TableDetailsDialog dialog1 = new TableDetailsDialog();
        dialog1.setDilogListener(new TableDetailsDialog.TableCreationQueryDialogListener() {
            @Override
            public void onDialogResult(String result) {
                if (!TextUtils.isEmpty(result)) {
                    /*PackageChooserDialog dialog = new PackageChooserDialog("Choose dir", project);
                    dialog.show();
                    String packageName = dialog.getSelectedPackage().getName();
                    Messages.showMessageDialog(project, packageName + "\n empty", "Information", Messages.getInformationIcon());*/
                    ModelCreatorWriteAction action = new ModelCreatorWriteAction(project, psiFile.getContainingDirectory(), result);
                    action.execute();
                } else {
                    Log.debug("Empty result");
                }
            }
        });
        dialog1.pack();
        dialog1.setSize(800, 400);
        dialog1.setVisible(true);
    }
}
