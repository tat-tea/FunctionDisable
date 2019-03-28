package jp.co.ncast.form;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class FunctionDisableForm extends JFrame implements ActionListener {

	JTextArea textArea;
	JCheckBox chckbx_Workflow;
	JCheckBox chckbx_Validation;
	JCheckBox chckbx_Process;
	JCheckBox chckbx_Trigger;
	JTextField metaDirName;

	private static final String btn_file_choose = "btn_file_choose";
	private static final String btn_disable = "btn_disable";
	private static final String btn_enable = "btn_enable";

	private JButton button_1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FunctionDisableForm frame = new FunctionDisableForm();
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
	public FunctionDisableForm() {
		setTitle("機能無効化くん_ver1.0");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 460, 368);
		getContentPane().setLayout(null);

		metaDirName = new JTextField();
		metaDirName.setBounds(12, 27, 355, 19);
		getContentPane().add(metaDirName);
		metaDirName.setColumns(10);

		chckbx_Trigger = new JCheckBox("Apexトリガ");
		chckbx_Trigger.setSelected(true);
		chckbx_Trigger.setBounds(67, 102, 137, 21);
		getContentPane().add(chckbx_Trigger);

		chckbx_Process = new JCheckBox("プロセスビルダ");
		chckbx_Process.setSelected(true);
		chckbx_Process.setBounds(67, 135, 137, 21);
		getContentPane().add(chckbx_Process);

		chckbx_Workflow = new JCheckBox("ワークフロー");
		chckbx_Workflow.setSelected(true);
		chckbx_Workflow.setBounds(241, 135, 135, 21);
		getContentPane().add(chckbx_Workflow);

		chckbx_Validation = new JCheckBox("入力規則");
		chckbx_Validation.setSelected(true);
		chckbx_Validation.setBounds(240, 103, 174, 21);
		getContentPane().add(chckbx_Validation);

		JLabel label = new JLabel("メタデータ格納先フォルダ");
		label.setBounds(12, 10, 164, 13);
		getContentPane().add(label);

		JButton button = new JButton("参照");
		button.setBounds(373, 26, 61, 21);
		button.addActionListener(this);
		button.setActionCommand(btn_file_choose);
		getContentPane().add(button);

		JLabel label_1 = new JLabel("無効化対象機能");
		label_1.setBounds(12, 78, 103, 13);
		getContentPane().add(label_1);

		textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setBounds(12, 269, 422, 52);
		getContentPane().add(textArea);

		JLabel label_2 = new JLabel("処理結果");
		label_2.setBounds(12, 246, 78, 13);
		getContentPane().add(label_2);

		button_1 = new JButton("無効化");
		button_1.setActionCommand(btn_disable);
		button_1.setBounds(67, 196, 131, 30);
		button_1.addActionListener(this);
		getContentPane().add(button_1);

		JButton btnGenerate = new JButton("有効化");
		btnGenerate.setBounds(223, 196, 131, 30);
		btnGenerate.setActionCommand(btn_enable);
		btnGenerate.addActionListener(this);
		getContentPane().add(btnGenerate);

	}

	// ボタン押下処理
	public void actionPerformed(ActionEvent e) {

		try {
			// 参照ボタン
			if (btn_file_choose.equals(e.getActionCommand())) {

				JFileChooser filechooser = new JFileChooser("c:¥¥temp");
				filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int selected = filechooser.showSaveDialog(this);
				if (selected == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					metaDirName.setText(file.getAbsolutePath());
				}
				// generateボタン
			} else if (btn_disable.equals(e.getActionCommand())) {

				// 入力チェック
				checkInput();

				ButtonAction action = new ButtonAction(getParam(null));
				action.editMetaData(false);

				textArea.setText("正常に処理が終了しました。");

			} else if (btn_enable.equals(e.getActionCommand())) {

				// 入力チェック
				checkInput();

				JOptionPane.showMessageDialog(null, "機能無効化リスト（success.csvを指定してください。）", "確認", JOptionPane.INFORMATION_MESSAGE, null);

				// success.csvの選択
				JFileChooser filechooser = new JFileChooser(metaDirName.getText());
				filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				ArrayList<String> fileTypeList = new ArrayList<String>();
				fileTypeList.add("csv");
				filechooser.addChoosableFileFilter(new CustomFileFilter(fileTypeList));
				int selected = filechooser.showSaveDialog(this);

				if (selected == JFileChooser.APPROVE_OPTION) {

					// CSVファイルチェック
					File file = filechooser.getSelectedFile();

					// メタデータの無効化処理
					ButtonAction action = new ButtonAction(getParam(file));
					action.editMetaData(true);
					textArea.setText("正常に処理が終了しました。");
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			textArea.setText("【異常終了】\n" + ex.getMessage());
		}
	}

	private void checkInput() throws Exception {

		// メタデータ格納先が設定されていない場合はエラー
		if (metaDirName.getText() == null || metaDirName.getText().isEmpty()) {
			throw new Exception("メタデータ格納先を設定してください。");
		}
		// チェックがすべて外れている場合エラー
		if (!chckbx_Workflow.isSelected() && !chckbx_Validation.isSelected() && !chckbx_Process.isSelected()
				&& !chckbx_Trigger.isSelected()) {
			throw new Exception("権限付与の対象をチェックしてください。");
		}

	}

	private ButtonActionParameter getParam(File disableFileList) {

		// Profileメタデータ作成処理
		ButtonActionParameter param = new ButtonActionParameter();
		param.setMetaFilePath(metaDirName.getText());
		param.setChckbx_Workflow(chckbx_Workflow.isSelected());
		param.setChckbx_Validation(chckbx_Validation.isSelected());
		param.setChckbx_Process(chckbx_Process.isSelected());
		param.setChckbx_Trigger(chckbx_Trigger.isSelected());
		param.setDisableFileList(disableFileList);
		return param;
	}
}
