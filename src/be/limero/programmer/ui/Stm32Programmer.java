package be.limero.programmer.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import be.limero.programmer.Stm32Controller;
import be.limero.programmer.Stm32Model;

public class Stm32Programmer extends JFrame {

	private JPanel contentPane;
	private JTextField txtHost;
	private JTextField txtBinaryFile;
	private Stm32Model model;
	private ActorRef controller;
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

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 950, 602);
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
		txtHost.setText("localhost");
		txtHost.setBounds(90, 8, 249, 20);
		contentPane.add(txtHost);
		txtHost.setColumns(10);

		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (model.getConnected())
					controller.tell("disconnect", ActorRef.noSender());
				else
				{
					model.setHost(txtHost.getText());
					model.setPort(Integer.valueOf(textPort.getText()));
					controller.tell("connect", ActorRef.noSender());
				}
			}
		});
		btnConnect.setBounds(462, 7, 148, 23);
		contentPane.add(btnConnect);

		lblStatus = new JLabel("Status");
		lblStatus.setBounds(10, 539, 914, 14);
		contentPane.add(lblStatus);

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 517, 914, 14);
		contentPane.add(progressBar);

		txtLogging = new JTextArea();
		txtLogging.setText("Logging");
		txtLogging.setBounds(10, 177, 914, 329);
		contentPane.add(txtLogging);

		txtBinaryFile = new JTextField();
		txtBinaryFile.setBounds(90, 42, 518, 20);
		contentPane.add(txtBinaryFile);
		txtBinaryFile.setColumns(10);

		JLabel lblBinaryFile = new JLabel("Binary file");
		lblBinaryFile.setHorizontalAlignment(SwingConstants.RIGHT);
		lblBinaryFile.setBounds(10, 45, 70, 14);
		contentPane.add(lblBinaryFile);

		JButton btnBrowse = new JButton("Browse...");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			}
		});
		btnBrowse.setBounds(659, 41, 89, 23);
		contentPane.add(btnBrowse);

		JButton btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.tell("reset", null);
			}
		});
		btnReset.setBounds(10, 70, 89, 23);
		contentPane.add(btnReset);

		JButton btnGo = new JButton("Go");
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.tell("go", null);
			}
		});
		btnGo.setBounds(109, 70, 89, 23);
		contentPane.add(btnGo);

		JButton btnProgram = new JButton("Program");
		btnProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.tell("program", null);
			}
		});
		btnProgram.setBounds(208, 70, 89, 23);
		contentPane.add(btnProgram);

		JButton btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.tell("read", null);
			}
		});
		btnRead.setBounds(307, 70, 89, 23);
		contentPane.add(btnRead);

		JButton btnVerify = new JButton("Verify");
		btnVerify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.tell("verify", null);
			}
		});
		btnVerify.setBounds(406, 70, 89, 23);
		contentPane.add(btnVerify);

		JButton btnDoItAll = new JButton("Do it all");
		btnDoItAll.setBounds(505, 70, 89, 23);
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
		rdbtnGo.setBounds(600, 147, 109, 23);
		contentPane.add(rdbtnGo);

		lblDeviceInfo = new JLabel("BootloaderVersion");
		lblDeviceInfo.setBounds(10, 99, 584, 14);
		contentPane.add(lblDeviceInfo);

		JButton btnEnterBootloader = new JButton("Bootloader init");
		btnEnterBootloader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.tell("enterBootloader", null);
			}
		});
		btnEnterBootloader.setBounds(10, 121, 172, 23);
		contentPane.add(btnEnterBootloader);

		JButton btnGetid = new JButton("GetID");
		btnGetid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.tell("getId", null);
			}
		});
		btnGetid.setBounds(291, 121, 89, 23);
		contentPane.add(btnGetid);

		JButton btnGetversioncommands = new JButton("GetVersionCommands");
		btnGetversioncommands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.tell("getVersion", null);
			}
		});
		btnGetversioncommands.setBounds(390, 121, 150, 23);
		contentPane.add(btnGetversioncommands);

		JLabel lblPort = new JLabel("Port");
		lblPort.setBounds(349, 11, 46, 14);
		contentPane.add(lblPort);

		textPort = new JTextField();
		textPort.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				model.setPort(Integer.valueOf(textPort.getText()));
			}
		});
		textPort.setText("3881");
		textPort.setBounds(406, 8, 46, 20);
		contentPane.add(textPort);
		textPort.setColumns(10);
		
		JButton btnGet = new JButton("Get");
		btnGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.tell("getVersion", null);
			}
		});
		btnGet.setBounds(192, 121, 89, 23);
		contentPane.add(btnGet);

		model = new Stm32Model();
		controller = ActorSystem.create("System").actorOf(Props.create(Stm32Controller.class, this, model),
				"Stm32Controller");

	}

	public void updateView() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {

				if (model.getConnected()) {
					getBtnConnect().setText("Dicsonnect");
				} else {
					getBtnConnect().setText("Connect");
				}
				lblDeviceInfo.setText(model.getInfo());
				getLblStatus().setText(model.getStatus());
				getProgressBar().setValue(model.getProgress());
				txtLogging.setText(model.getLog());
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
