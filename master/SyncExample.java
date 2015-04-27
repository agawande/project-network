
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import java.util.Scanner;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.sync.ChronoSync2013;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class SyncExample extends JFrame implements ChronoSync2013.OnInitialized,
ChronoSync2013.OnReceivedSyncState,
OnData,
OnInterest,
OnRegisterFailed,
OnTimeout, KeyListener, CaretListener, ActionListener
{
    private ChronoSync2013 m_chronoSync;
    private Face m_face;
    private KeyChain m_keyChain;
    private Name m_certificateName;
    private int m_content = 1;
    private Map<String,Long> m_lastSequenceNo = new HashMap<String,Long>();
    private UUID m_uuid;

    private JPanel panel = new JPanel();

    JTextArea codeArea;
    JScrollPane codeAreaScroll;
    JButton invite = new JButton("Invite");

    Scanner input = new Scanner(System.in);
    int role;
    public SyncExample(Face face, String bprefix, int role) throws SecurityException
    {
        m_face = face;
        m_certificateName = new Name();

        this.role=role;

        // Set up KeyChain and Identity
        m_keyChain = Security.initialize(m_face, m_certificateName);

        // Used to make the data prefix unique to this process
        m_uuid = UUID.randomUUID();
        System.out.println(m_uuid);
        final Name DATA_PREFIX = new Name("/ndn/whiteboard/example/" + m_uuid);
        final Name BROADCAST_PREFIX = new Name(bprefix);

        int session = (int)Math.round(System.currentTimeMillis() / 1000.0);

        // Register Application prefix
        try {
            m_face.registerPrefix(DATA_PREFIX, this, this);
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        try {
            m_chronoSync = new ChronoSync2013(this,
                this,
                DATA_PREFIX,
                BROADCAST_PREFIX,
                session,
                m_face,
                m_keyChain,
                m_certificateName,
                2000,
                this);
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        setSize(600, 600);

        codeArea = new JTextArea("", 30, 50);
        codeArea.setBorder ( new TitledBorder ( new EtchedBorder (), "Display Area" ) );
        //codeArea.getDocument().addDocumentListener(this);
        codeArea.addKeyListener(this);
        codeArea.addCaretListener(this);

        codeAreaScroll = new JScrollPane(codeArea);  //, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        invite.addActionListener(this);
        panel.add(invite);
        panel.add(codeAreaScroll);
        add(panel);
        setVisible(true);

        if(role == 2){
            codeArea.setEditable(false);
        }

    }

    public void
    publish() throws IOException, SecurityException
    {
        m_content++;                                                                                                                                                                                                                                                                                                                                                                           
        m_chronoSync.publishNextSequenceNo();
    }
    int pos;
    int accept;
    boolean isAccepted = true;
    public void
    onData(Interest interest, Data data)
    {
        System.out.println("onData: " + interest.getName().toUri() + "\n");
        String keyP = data.getContent().toString();
        System.out.println(keyP);
        Document doc = codeArea.getDocument();

        if(keyP.contains("req/")&&role==1&&isAccepted)
        {
            System.out.println("Accept (1=Y, else=N)? ");
            accept = input.nextInt();
            if(accept==1)
            {
                keyPressed = "stop/"+keyP;
                System.out.println(keyPressed);
                isAccepted = false;
                codeArea.setEditable(false);
                codeArea.removeCaretListener(this);
                try{
                    publish();
                }
                catch(Exception f){};
            }
        }
        else if(keyP.contains("all-stop")&&role==1)
        {
            codeArea.setEditable(true);
            codeArea.addCaretListener(this);
            isAccepted = true;
        }
        else{
            if(!keyP.equals("1")&&!keyP.equals(""))
            {
                String[] contents = keyP.split("~");
                //if content==backspace (represented by single space)
                System.out.println("Length: "+contents.length);
                if(contents.length==3)
                {
                    pos=Integer.parseInt(contents[1]);
                    System.out.println(pos);
                    codeArea.insert(contents[0], pos-1);
                    codeArea.setCaretPosition(pos);
                }
                else if(contents.length==1)
                {
                    if((contents[0]).equals("\u0008"))
                    {
                        System.out.println("Backspace!");
                        try{
                            doc.remove(codeArea.getText().length()-1, 1);
                        } catch(Exception e){

                        }
                    }
                }
                else
                {
                    System.out.println(Integer.parseInt(contents[0]));
                    int pos = Integer.parseInt(contents[0]);
                    codeArea.setCaretPosition(pos);
                }
                codeArea.update(codeArea.getGraphics());
            }
        }
    }

    public void
    onTimeout(Interest interest)
    {
        // pass
    }
    String tr = "";
    public void
    onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId)
    {
        System.out.println("onInterest: " + interest.getName().toUri() + "\n");

        // Create response Data
        Data data = new Data(interest.getName());
        //Name code = new Name(codeArea.getText());
        //System.out.println(codeArea.getText());
        //Data data = new Data(code);
        //data.setContent(new Blob(tr));
        data.setContent(new Blob(keyPressed));
        //tr="";
        keyPressed="";
        Blob encodedData = data.wireEncode();

        // Send Data
        try {
            System.out.println("Sending Data for " + interest.getName().toUri());
            transport.send(encodedData.buf());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        //Publish new Data
        //try {
        //  publish();
        //}
        //catch (Exception e){;}
    }

    public final void
    onInitialized()
    {
        // pass
    }

    public final void
    onReceivedSyncState(List syncStates, boolean isRecovery)
    {
        System.out.println("On Recieved Sync State \n");
        // Iterate through SyncState updates
        for (int i = 0; i < syncStates.size(); ++i) {
            ChronoSync2013.SyncState state = (ChronoSync2013.SyncState)syncStates.get(i);

            // Get UUID associated with SyncState
            String id = new Name(state.getDataPrefix()).get(-1).toEscapedString();

            // Don't fetch own updates
            if (id.equals(m_uuid.toString())) {
                continue;
            }

            // Don't fetch outdated data
            if (m_lastSequenceNo.get(id) != null && state.getSequenceNo() <= m_lastSequenceNo.get(id)) {
                continue;
            }

            m_lastSequenceNo.put(id, state.getSequenceNo());

            // Construct Interest
            Name name = new Name(state.getDataPrefix() + "/" + state.getSequenceNo());
            Interest interest = new Interest(name);

            try {
                System.out.println("Expressing Interest for " + name.toUri());
                m_face.expressInterest(interest, this, this);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void
    onRegisterFailed(Name prefix)
    {
        throw new Error("Prefix registration failed");
    }
    /*
    public void insertUpdate(DocumentEvent e)
    {
    //System.out.println("Publishing data");
    //tr=codeArea.getText().substring(codeArea.getText().length()-1);
    //System.out.println(e.getKeyCode());
    try {
    publish();
    } catch (Exception f){;}
    }

    public void removeUpdate(DocumentEvent e)
    {
    //changed = true;
    }

    public void changedUpdate(DocumentEvent e)
    {
    //changed = true;
    }*/
    String keyPressed="";
    @Override
    public void keyPressed(KeyEvent e){

    }

    @Override
    public void keyTyped(KeyEvent e) {
        System.out.println("Key Pressed: "+e.getKeyChar());
        //System.out.println("Key code: " +(int)e.getKeyCode());
        if(e.getKeyChar() == KeyEvent.VK_ENTER)
        {
            keyPressed = "\n~";
        }
        else
        {
            String pressed=""+e.getKeyChar();
            //System.out.println(codeArea.getCaretPosition());
            keyPressed = pressed+"~";
        }
        try {
            publish();
        } catch (Exception f){;}
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void caretUpdate(CaretEvent e) {
        System.out.println("Cursor position: "+e.getDot());
        keyPressed += e.getDot()+"~cursor";
        try {
            publish();
        } catch (Exception f){;}
    }

    public void actionPerformed(ActionEvent e) { 
        //codeArea.setEditable(false);
        //keyPressed = "invite";
        //codeArea.removeCaretListener(this);
        //try {
        //    publish();
        //} catch (Exception f){;}
    }

    public static void
    main(String[] argv)
    {
        //Scanner input = new Scanner(System.in);
        //System.out.println("Please choose role (1 = Professor, 2 = Student): ");
        //int role = input.nextInt();        

        //System.out.println("Please enter boradcast prefix: ");
        //String bprefix = input.next();

        int role = 1;
        String bprefix = "/ndn/broadcast/whiteboard";

        try {
            Face face = new Face();

            SyncExample sync = new SyncExample(face, bprefix, role);

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