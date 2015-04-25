/**
 * Write a description of class Client here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
public class Client extends JFrame 
{

    //private Face m_face;
    //private final Name m_prefix;

    //private int m_version;
    
    private JPanel panel = new JPanel();

    JTextArea codeArea;
    JScrollPane codeAreaScroll;
    JButton start = new JButton("Start");
    JButton pause = new JButton("Pause");

    public Client(){
        setSize(600, 600);

        //m_face = face;
        //m_prefix = prefix;
        
        codeArea = new JTextArea("This is an editable JTextArea.", 30, 50);
        codeArea.setBorder ( new TitledBorder ( new EtchedBorder (), "Display Area" ) );

        codeAreaScroll = new JScrollPane(codeArea);  //, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        panel.add(start);
        panel.add(pause);
        panel.add(codeAreaScroll);
        add(panel);
        setVisible(true);
    }

    public static void main(String[] args)
    {
      try {
      Face face = new Face();

      SyncExample sync = new SyncExample(face);
      Client c1 = new Client();

      while (true) {
        face.processEvents();

        // We need to sleep for a few milliseconds so we don't use 100% of
        // the CPU.
        Thread.sleep(5);
      }
    }
    catch (Exception e) {
      System.out.println("exception: " + e.getMessage());
    }
    }
}