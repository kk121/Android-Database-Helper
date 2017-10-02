import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Created by krishna on 30/09/17.
 */
public class ModelCreator extends EditorAction {
    protected ModelCreator(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    public ModelCreator() {
        super(new ModelCreatorHandler());

    }

    public static class ModelCreatorHandler extends EditorActionHandler {

        @Override
        protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            super.doExecute(editor, caret, dataContext);
        }
    }
}
