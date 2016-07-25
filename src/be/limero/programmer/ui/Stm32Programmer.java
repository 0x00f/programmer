package be.limero.programmer.ui;


import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import be.limero.file.FileManager;
import be.limero.programmer.Stm32Model;
import be.limero.vertx.Controller;

public class Stm32Programmer extends JFrame {

	private JPanel contentPane;
	private JTextField txtHost;
	private JTextField txtBinaryFile;
	private Stm32Model model;
	private LogHandler logHandler;
	// private EventBus eb = Vertx.factory.vertx().eventBus();
	private Controller controller;
	private JLabel lblDeviceInfo;
	private JRadioButton rdbtnReset;
	private JRadioButton rdbtnProgram;
	private JRadioButton rdbtnVerify;
	private JRadioButton rdbtnGo;
	private JLabel lblStatus;
	private JProgressBar progressBar;
	private JTextArea txtLogging;
	private JButton btnConnect;
	private JTextField textPort;
	final JFileChooser fc = new JFileChooser();
	private final static Logger log = Logger.getLogger(Stm32Programmer.class.toString());




	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Stm32Programmer frame = new Stm32Programmer();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Stm32Programmer() {

		model = new Stm32Model();
		controller = new Controller(this, model);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 716, 477);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblMqttHostConnection = new JLabel("Host");
		lblMqttHostConnection.setHorizontalAlignment(SwingConstants.RIGHT);
		lblMqttHostConnection.setBounds(53, 11, 27, 14);
		contentPane.add(lblMqttHostConnection);

		txtHost = new JTextField();
		txtHost.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				model.setHost(txtHost.getText());
			}
		});
		txtHost.setText("iot.eclipse.org");
		txtHost.setBounds(90, 8, 141, 20);
		contentPane.add(txtHost);
		txtHost.setColumns(10);

		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (model.getConnected())
					controller.send("disconnect");
				else {
					model.setHost(txtHost.getText());
					model.setPort(Integer.valueOf(textPort.getText()));
					controller.send("connect");
				}
			}
		});
		btnConnect.setBounds(349, 7, 109, 23);
		contentPane.add(btnConnect);

		lblStatus = new JLabel("Status");
		lblStatus.setBounds(10, 421, 688, 14);
		contentPane.add(lblStatus);

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 396, 688, 14);
		contentPane.add(progressBar);

		txtBinaryFile = new JTextField();
		txtBinaryFile.setBounds(90, 39, 386, 20);
		contentPane.add(txtBinaryFile);
		txtBinaryFile.setColumns(10);

		JLabel lblBinaryFile = new JLabel("Binary file");
		lblBinaryFile.setHorizontalAlignment(SwingConstants.RIGHT);
		lblBinaryFile.setBounds(10, 45, 70, 14);
		contentPane.add(lblBinaryFile);

		JButton btnBrowse = new JButton("Browse...");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Handle open button action.
			    if (e.getSource() == btnBrowse) {
			        int returnVal = fc.showOpenDialog(Stm32Programmer.this);

			        if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            //This is where a real application would open the file.
			            log.info("Opening: " + file.getName() );
			            model.setBinFile(file.getAbsolutePath());
			            updateView();
			        } else {
			            log.info("Open command cancelled by user.");
			        }
			   }
			}
		});
		btnBrowse.setBounds(486, 38, 89, 23);
		contentPane.add(btnBrowse);

		JButton btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("reset");
			}
		});
		btnReset.setBounds(10, 70, 63, 23);
		contentPane.add(btnReset);

		JButton btnGo = new JButton("Go");
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("go");
			}
		});
		btnGo.setBounds(81, 70, 56, 23);
		contentPane.add(btnGo);

		JButton btnProgram = new JButton("Program");
		btnProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("program");
			}
		});
		btnProgram.setBounds(147, 70, 75, 23);
		contentPane.add(btnProgram);

		JButton btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("read");
			}
		});
		btnRead.setBounds(232, 70, 63, 23);
		contentPane.add(btnRead);

		JButton btnVerify = new JButton("Verify");
		btnVerify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("verify");
			}
		});
		btnVerify.setBounds(305, 70, 70, 23);
		contentPane.add(btnVerify);

		JButton btnDoItAll = new JButton("Do it all");
		btnDoItAll.setBounds(385, 70, 89, 23);
		contentPane.add(btnDoItAll);

		rdbtnReset = new JRadioButton("Reset");
		rdbtnReset.setBounds(600, 69, 109, 23);
		contentPane.add(rdbtnReset);

		rdbtnProgram = new JRadioButton("Program");
		rdbtnProgram.setBounds(600, 95, 109, 23);
		contentPane.add(rdbtnProgram);

		rdbtnVerify = new JRadioButton("Verify");
		rdbtnVerify.setBounds(600, 121, 109, 23);
		contentPane.add(rdbtnVerify);

		rdbtnGo = new JRadioButton("Go");
		rdbtnGo.setBounds(600, 41, 109, 23);
		contentPane.add(rdbtnGo);

		lblDeviceInfo = new JLabel("BootloaderVersion");
		lblDeviceInfo.setBounds(10, 99, 584, 14);
		contentPane.add(lblDeviceInfo);

		JButton btnEnterBootloader = new JButton("Bootloader init");
		btnEnterBootloader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("enterBootloader");
			}
		});
		btnEnterBootloader.setBounds(10, 121, 172, 23);
		contentPane.add(btnEnterBootloader);

		JButton btnGetid = new JButton("GetID");
		btnGetid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("getId");
			}
		});
		btnGetid.setBounds(291, 121, 89, 23);
		contentPane.add(btnGetid);

		JButton btnGetversioncommands = new JButton("GetVersionCommands");
		btnGetversioncommands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("getVersion");
			}
		});
		btnGetversioncommands.setBounds(390, 121, 150, 23);
		contentPane.add(btnGetversioncommands);

		JLabel lblPort = new JLabel("Port");
		lblPort.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPort.setBounds(235, 11, 46, 14);
		contentPane.add(lblPort);

		textPort = new JTextField();
		textPort.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				model.setPort(Integer.valueOf(textPort.getText()));
			}
		});
		textPort.setText("1883");
		textPort.setBounds(293, 8, 46, 20);
		contentPane.add(textPort);
		textPort.setColumns(10);

		JButton btnGet = new JButton("Get");
		btnGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("get");
			}
		});
		btnGet.setBounds(192, 121, 89, 23);
		contentPane.add(btnGet);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 162, 688, 223);
		contentPane.add(scrollPane);
		
				txtLogging = new JTextArea();
				txtLogging.setFont(new Font("Monospaced", Font.PLAIN, 11));
				scrollPane.setViewportView(txtLogging);
				txtLogging.setText("Logging");

	}

	public void updateView() {
		EventQueue.invokeLater(new Runnable() {

			public void run() {

				if (model.getConnected()) {
					getBtnConnect().setText("Disconnect");
				} else {
					getBtnConnect().setText("Connect");
				}
				lblDeviceInfo.setText(model.getInfo());
				getLblStatus().setText(model.getStatus());
				getProgressBar().setValue(model.getProgress());
				txtLogging.setText(model.getLog());
				txtBinaryFile.setText(model.getBinFile());
			}

		});
	}

	protected JLabel getLblBootloaderversion() {
		return lblDeviceInfo;
	}

	public JTextField getTxtMqttConnection() {
		return txtHost;
	}

	public JTextField getTxtBinaryFile() {
		return txtBinaryFile;
	}

	public JRadioButton getRdbtnReset() {
		return rdbtnReset;
	}

	public JRadioButton getRdbtnProgram() {
		return rdbtnProgram;
	}

	public JRadioButton getRdbtnVerify() {
		return rdbtnVerify;
	}

	public JRadioButton getRdbtnGo() {
		return rdbtnGo;
	}

	public JLabel getLblStatus() {
		return lblStatus;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public JTextArea getTxtLogging() {
		return txtLogging;
	}

	public JButton getBtnConnect() {
		return btnConnect;
	}
}
