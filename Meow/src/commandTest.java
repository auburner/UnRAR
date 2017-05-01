import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class commandTest {
	
public static void main(String[] args) {
    String command="netstat";
    try {
        Process process = Runtime.getRuntime().exec(command);
        System.out.println("the output stream is "+process.getOutputStream());
        BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
        String s; 
        while ((s = reader.readLine()) != null){
            System.out.println("The inout stream is " + s);
        }                   
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
