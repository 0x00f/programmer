package be.limero.programmer.ui;

import java.awt.Color;
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
import be.limero.programmer.Stm32Model.Verification;
import be.limero.vertx.Controller;
import io.vertx.core.json.JsonObject;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

public class Stm32Programmer extends JFrame {

	private JPanel contentPane;
	private JTextField txtHost;
	private JTextField txtBinaryFile;
	private LogHandler logHandler;
	// private EventBus eb = Vertx.factory.vertx().eventBus();
	private Controller controller;
	private JLabel lblDeviceInfo;
	private JLabel lblStatus;
	private JProgressBar progressBar;
	private JTextArea txtLogging;
	private JButton btnConnect;
	private JTextField textPort;
	final JFileChooser fc = new JFileChooser();
	private final static Logger log = Logger.getLogger(Stm32Programmer.class.toString());
	private JButton btnReset;
	private JButton btnGo;
	private JButton btnProgram;
	private JButton btnRead;
	private JButton btnGetversioncommands;
	private JButton btnVerify;
	/**
	 * @wbp.nonvisual location=19,511
	 */
	private final Stm32Model model = new Stm32Model();
	private JTextArea txtUart;
	private JButton btnErase;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT.%1$tL %4$s %2$s %5$s%6$s%n");
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

		controller = new Controller(this);

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
		txtHost.setText("192.168.0.132");
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
		lblStatus.setBounds(235, 425, 463, 14);
		contentPane.add(lblStatus);

		progressBar = new JProgressBar();
		progressBar.setValue(10);
		progressBar.setBounds(10, 396, 688, 14);
		contentPane.add(progressBar);

		txtBinaryFile = new JTextField();
		txtBinaryFile.setText(
				"C:\\Users\\lieven2\\Atollic\\TrueSTUDIO\\ARM_workspace_5.4\\opencm3\\Debug\\opencm3.elf.binary");
		txtBinaryFile.setBounds(90, 39, 386, 20);
		model.setBinFile(txtBinaryFile.getText());
		contentPane.add(txtBinaryFile);
		txtBinaryFile.setColumns(10);

		JLabel lblBinaryFile = new JLabel("Binary file");
		lblBinaryFile.setHorizontalAlignment(SwingConstants.RIGHT);
		lblBinaryFile.setBounds(10, 45, 70, 14);
		contentPane.add(lblBinaryFile);

		JButton btnBrowse = new JButton("Browse...");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Handle open button action.
				if (e.getSource() == btnBrowse) {
					int returnVal = fc.showOpenDialog(Stm32Programmer.this);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						// This is where a real application would open the file.
						log.info("Opening: " + file.getName());
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

		btnReset = new JButton("resetBootloader");
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("resetBootloader");
			}
		});
		btnReset.setBounds(10, 70, 127, 23);
		contentPane.add(btnReset);

		btnGo = new JButton("Go");
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("goFlash");
			}
		});
		btnGo.setBounds(147, 104, 56, 23);
		contentPane.add(btnGo);

		btnProgram = new JButton("Program");
		btnProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("program");
			}
		});
		btnProgram.setBounds(247, 70, 75, 23);
		contentPane.add(btnProgram);

		btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("read");
			}
		});
		btnRead.setBounds(412, 70, 63, 23);
		contentPane.add(btnRead);

		btnVerify = new JButton("Verify");
		btnVerify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("verify");
			}
		});
		btnVerify.setBounds(332, 70, 70, 23);
		contentPane.add(btnVerify);

		JButton btnDoItAll = new JButton("Status");
		btnDoItAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("status");
			}
		});
		btnDoItAll.setBounds(332, 104, 89, 23);
		contentPane.add(btnDoItAll);

		lblDeviceInfo = new JLabel("deviceInfo");
		lblDeviceInfo.setBounds(10, 425, 215, 14);
		contentPane.add(lblDeviceInfo);

		JButton btnEnterBootloader = new JButton("resetFlash");
		btnEnterBootloader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("resetFlash");
			}
		});
		btnEnterBootloader.setBounds(10, 104, 127, 23);
		contentPane.add(btnEnterBootloader);

		btnGetversioncommands = new JButton("Program & Start");
		btnGetversioncommands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				controller.send("autoProgram");
			}
		});
		btnGetversioncommands.setBounds(213, 104, 109, 23);
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

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 162, 688, 95);
		contentPane.add(scrollPane);

		txtLogging = new JTextArea();
		txtLogging.setRows(1000);
		txtLogging.setFont(new Font("Monospaced", Font.PLAIN, 11));
		scrollPane.setViewportView(txtLogging);
		txtLogging.setText("Logging");

		btnErase = new JButton("Erase");
		btnErase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.send("erase");
			}
		});
		btnErase.setBounds(147, 70, 91, 23);
		contentPane.add(btnErase);

		JCheckBox chckbxAutoprogram = new JCheckBox("AutoProgram");
		chckbxAutoprogram.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getStm32Model().setAutoProgram(chckbxAutoprogram.isSelected());

			}
		});
		chckbxAutoprogram.setBounds(486, 70, 97, 23);
		contentPane.add(chckbxAutoprogram);

		JComboBox<String> cbBaudrate = new JComboBox<String>();
		cbBaudrate.setModel(new DefaultComboBoxModel<String>(
				new String[] { "23800", "57600", "115200", "230400", "460800", "921600" }));
		cbBaudrate.setSelectedIndex(2);
		cbBaudrate.setBounds(515, 104, 107, 22);
		cbBaudrate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getStm32Model().setBaudrate(Integer.valueOf((String) cbBaudrate.getSelectedItem()));
				controller.send("baudrate");

			}
		});
		contentPane.add(cbBaudrate);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 268, 688, 117);
		contentPane.add(scrollPane_1);

		txtUart = new JTextArea();
		txtUart.setRows(1000);
		scrollPane_1.setViewportView(txtUart);
		
		JLabel lblUartBaudrate = new JLabel("UART Baudrate");
		lblUartBaudrate.setBounds(430, 108, 75, 14);
		contentPane.add(lblUartBaudrate);

	}

	public void updateView() {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				boolean enableButton = false;
				if (model.getConnected()) {
					enableButton = true;
					getBtnConnect().setText("Disconnect");
				} else {
					getBtnConnect().setText("Connect");
				}
				getBtnGetversioncommands().setEnabled(enableButton);
				getBtnGo().setEnabled(enableButton);
				getBtnProgram().setEnabled(enableButton);
				getBtnRead().setEnabled(enableButton);
				getBtnReset().setEnabled(enableButton);
				getBtnVerify().setEnabled(enableButton);
				getBtnErase().setEnabled(enableButton && (model.getCommands() != null ));

				getLblStatus().setText(model.getStatus().toString());
				getProgressBar().setValue(model.getProgress());
				getLblDeviceInfo().setText(model.getDeviceInfo());
		

				txtBinaryFile.setText(model.getBinFile());
				if (model.getVerification() == Verification.OK) {
					getBtnVerify().setBackground(Color.GREEN);
				} else if (model.getVerification() == Verification.FAIL) {
					getBtnVerify().setBackground(Color.RED);
				} else if (model.getVerification() == Verification.NA) {
					getBtnVerify().setBackground(Color.GRAY);
				}
			}

		});
	}

	public void addLog(String type, String text) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				if (type == "local") {
					txtLogging.append(text);
                    int len = txtLogging.getDocument().getLength();
                    txtLogging.setCaretPosition(len);
				}
				if (type == "remote") {
					txtUart.append(text);
					int len = txtUart.getDocument().getLength();
					txtUart.setCaretPosition(len);
				}
			}
		});
	}

	protected JLabel getLblDeviceInfo() {
		return lblDeviceInfo;
	}

	public JTextField getTxtMqttConnection() {
		return txtHost;
	}

	public JTextField getTxtBinaryFile() {
		return txtBinaryFile;
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

	public JButton getBtnReset() {
		return btnReset;
	}

	public JButton getBtnGo() {
		return btnGo;
	}

	public JButton getBtnProgram() {
		return btnProgram;
	}

	public JButton getBtnRead() {
		return btnRead;
	}


	public JButton getBtnGetversioncommands() {
		return btnGetversioncommands;
	}

	public JButton getBtnVerify() {
		return btnVerify;
	}

	public Stm32Model getStm32Model() {
		return model;
	}

	public JTextArea getTxtUart() {
		return txtUart;
	}
	public JButton getBtnErase() {
		return btnErase;
	}
}
