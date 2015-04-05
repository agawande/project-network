
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
import java.nio.ByteBuffer;

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

public class SyncExample extends JFrame implements ChronoSync2013.OnInitialized,
ChronoSync2013.OnReceivedSyncState,
OnData,
OnInterest,
OnRegisterFailed,
OnTimeout, DocumentListener
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
    JButton start = new JButton("Start");
    JButton pause = new JButton("Pause");

    public SyncExample(Face face) throws SecurityException
    {
        m_face = face;
        m_certificateName = new Name();

        // Set up KeyChain and Identity
        m_keyChain = Security.initialize(m_face, m_certificateName);

        // Used to make the data prefix unique to this process
        m_uuid = UUID.randomUUID();
        System.out.println(m_uuid);
        final Name DATA_PREFIX = new Name("/ndn/whiteboard/example/" + m_uuid);
        final Name BROADCAST_PREFIX = new Name("/ndn/broadcast/whiteboard");

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
        codeArea.getDocument().addDocumentListener(this);

        codeAreaScroll = new JScrollPane(codeArea);  //, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        panel.add(start);
        panel.add(pause);
        panel.add(codeAreaScroll);
        add(panel);
        setVisible(true);

        codeArea.addKeyListener(new KeyListener(){
                @Override
                public void keyPressed(KeyEvent e){
                    if(e.getKeyCode() == KeyEvent.VK_ENTER){
                        codeArea.append("\n");
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }
            });
    }

    public void
    publish() throws IOException, SecurityException
    {
        m_content++;
        m_chronoSync.publishNextSequenceNo();
    }

    public void
    onData(Interest interest, Data data)
    {        
        System.out.println("onData: " + interest.getName().toUri() + "\n");
        //codeArea.append("onData: " + interest.getName().toUri() + "\n");
        //codeArea.append(data+"\n");
        String keyPressed = data.getContent().toString();
        System.out.println(keyPressed);
        Document doc = codeArea.getDocument();
        //SimpleAttributeSet attributes = new SimpleAttributeSet();
        //try{
        //    doc.insertString(doc.getLength(), keyPressed, attributes);
        //} catch(Exception e){}

        if(!keyPressed.equals(""))
        {
            String[] contents = keyPressed.split("~");
            if((contents[1]).equals("8"))
            { 
                //codeArea.setText(codeArea.getText().substring(0, codeArea.getText().length()-1));
                try{
                    doc.remove(codeArea.getText().length()-1, 1);
                } catch(Exception e){

                }
            }
            else
            {
                codeArea.append(contents[0]);
            }
            codeArea.update(codeArea.getGraphics());
        }
        //m_content = Integer.parseInt(data.getContent().toString());
    }

    public void
    onTimeout(Interest interest)
    {
        System.out.println("timeout\n");
    }

    public void
    onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId)
    {
        //System.out.println("onInterest: " + interest.getName().toUri() + "\n");

        // Create response Data
        Data data = new Data(interest.getName());
        data.setContent(new Blob(Integer.toString(m_content)));

        Blob encodedData = data.wireEncode();

        // Send Data
        try {
            System.out.println("Sending Data for " + interest.getName().toUri());
            transport.send(encodedData.buf());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // Publish new Data
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

    public void insertUpdate(DocumentEvent e)
    {
        //something has been inserted into the text box
        //try {
        //  publish();
        //} catch (Exception f){;}
    }

    public void removeUpdate(DocumentEvent e)
    {
        //changed = true;
    }

    public void changedUpdate(DocumentEvent e)
    {
        //changed = true;
    }

    public static void
    main(String[] argv)
    {
        try {
            Face face = new Face();

            SyncExample sync = new SyncExample(face);

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