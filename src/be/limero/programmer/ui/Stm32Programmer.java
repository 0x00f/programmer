package be.limero.programmer.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;

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

import be.limero.programmer.Stm32Controller;
import be.limero.programmer.Stm32Model;

public class Stm32Programmer extends JFrame {

	private JPanel contentPane;
	private JTextField txtMqttConnection;
	private JTextField txtMqttPrefix;
	private JTextField txtBinaryFile;
	private Stm32Model model;
	private Stm32Controller controller;
	private JLabel lblDeviceInfo;
	private JRadioButton rdbtnReset;
	private JRadioButton rdbtnProgram;
	private JRadioButton rdbtnVerify;
	private JRadioButton rdbtnGo;
	private JLabel lblStatus;
	private JProgressBar progressBar;
	private JTextArea txtLogging;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
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
		
		JLabel lblMqttHostConnection = new JLabel("MQTT Host connection ");
		lblMqttHostConnection.setBounds(10, 11, 111, 14);
		contentPane.add(lblMqttHostConnection);
		
		txtMqttConnection = new JTextField();
		txtMqttConnection.setText("tcp://iot.eclipse.org:1883");
		txtMqttConnection.setBounds(131, 8, 249, 20);
		contentPane.add(txtMqttConnection);
		txtMqttConnection.setColumns(10);
		
		JLabel lblPrefixStm = new JLabel("Prefix device :");
		lblPrefixStm.setBounds(390, 11, 77, 14);
		contentPane.add(lblPrefixStm);
		
		txtMqttPrefix = new JTextField();
		txtMqttPrefix.setText("limero314/ESP_00072740/");
		txtMqttPrefix.setBounds(477, 8, 172, 20);
		contentPane.add(txtMqttPrefix);
		txtMqttPrefix.setColumns(10);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.connect();
			}
		});
		btnConnect.setBounds(659, 7, 89, 23);
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
		txtBinaryFile.setBounds(131, 42, 518, 20);
		contentPane.add(txtBinaryFile);
		txtBinaryFile.setColumns(10);
		
		JLabel lblBinaryFile = new JLabel("Binary file");
		lblBinaryFile.setHorizontalAlignment(SwingConstants.RIGHT);
		lblBinaryFile.setBounds(10, 45, 111, 14);
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
				controller.reset();
			}
		});
		btnReset.setBounds(10, 70, 89, 23);
		contentPane.add(btnReset);
		
		JButton btnGo = new JButton("Go");
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.go();
			}
		});
		btnGo.setBounds(109, 70, 89, 23);
		contentPane.add(btnGo);
		
		JButton btnProgram = new JButton("Program");
		btnProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.program();
			}
		});
		btnProgram.setBounds(208, 70, 89, 23);
		contentPane.add(btnProgram);
		
		JButton btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.readMemory();
			}
		});
		btnRead.setBounds(307, 70, 89, 23);
		contentPane.add(btnRead);
		
		JButton btnVerify = new JButton("Verify");
		btnVerify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.verify();
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
				controller.enterBootloader();
			}
		});
		btnEnterBootloader.setBounds(10, 121, 172, 23);
		contentPane.add(btnEnterBootloader);
		
		JButton btnGetid = new JButton("GetID");
		btnGetid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.getId();
			}
		});
		btnGetid.setBounds(192, 121, 89, 23);
		contentPane.add(btnGetid);
		
		JButton btnGetversioncommands = new JButton("GetVersionCommands");
		btnGetversioncommands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.getVersionCommands();
			}
		});
		btnGetversioncommands.setBounds(291, 121, 150, 23);
		contentPane.add(btnGetversioncommands);

		controller=new Stm32Controller(this);
		model = controller.getModel();
	}
	
	public void updateView() {
		EventQueue.invokeLater(new Runnable(){

			@Override
			public void run() {
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
		return txtMqttConnection;
	}
	public JTextField getTxtMqttPrefix() {
		return txtMqttPrefix;
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
}
