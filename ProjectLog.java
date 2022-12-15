import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Date;

/*Helper class to print the server logs*/
public class ProjectLog {

    private String className;
    private PrintStream printStream;

    private void printMessage(String msg) {
        String date = new Timestamp(new Date().getTime()).toString();
        try {
            printStream.println("PROJECT LOG: " + date + " : " + this.className + " :" + msg);
        } catch (Exception e) {
            System.out.print("Exception:: " + e.getMessage());
        }
    }

    public void logMessage(String msg) {
        this.printMessage("- " + msg);

    }

    public ProjectLog(String className, PrintStream printStream) {
        this.className = className;
        this.printStream = printStream;
    }
}
