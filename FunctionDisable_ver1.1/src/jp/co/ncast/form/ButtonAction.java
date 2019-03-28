package jp.co.ncast.form;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ButtonAction {

	private static ArrayList<String> targetTypeList = new ArrayList<String>();
	private static ArrayList<String> fieldList = new ArrayList<String>();
	private ButtonActionParameter param = null;

	// 無効化にした機能の一覧情報。復活するためのlstファイルを吐き出す。
	private class DisableList {
		ArrayList<String> triggerList = new ArrayList<String>();
		ArrayList<String> validationList = new ArrayList<String>();
		ArrayList<String> processList = new ArrayList<String>();
		HashMap<String, String> processVersion = new HashMap<String, String>();
		ArrayList<String> workFlowList = new ArrayList<String>();
	}

	static {
		// 除外リスト
		targetTypeList.add("MasterDetail");

		// 除外リスト
		fieldList.add("Name");
		fieldList.add("OwnerId");
		fieldList.add("Status");
		fieldList.add("Priority");
	}

	public ButtonAction(ButtonActionParameter param) {
		this.param = param;
	}

	/**
	 * @param true:有効化
	 *            false:無効化
	 */
	public void editMetaData(Boolean isActive) throws Exception {
		try {

			// 無効化リストを作成
			DisableList disList = new DisableList();

			if (isActive) {
				setProcessVersion(disList);
			}

			// XMLロードの準備
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xmlDocument = builder.newDocument();

			// APEXトリガの無効化
			if (param.isChckbx_Trigger()) {
				disableTrigger(xmlDocument, builder, disList, isActive);
			}

			// 入力規則の無効化
			if (param.isChckbx_Validation()) {
				disableValidation(xmlDocument, builder, disList, isActive);
			}

			// プロセスビルダーの無効化
			if (param.isChckbx_Validation()) {
				disableProcessBuilder(xmlDocument, builder, disList, isActive);
			}

			// ワークフローの無効化
			if (param.isChckbx_Validation()) {
				disableWorkFlow(xmlDocument, builder, disList, isActive);
			}

			// 成功した機能リストをCSV形式で出力
			exportCSV(disList);

		} catch (Exception ex) {
			throw ex;
		}
	}

	// APEXトリガの無効化処理
	private void disableTrigger(Document xmlDocument, DocumentBuilder documentBuilder, DisableList disList,
			Boolean isActive) throws Exception {

		// パスからトリガのXMLファイルリストを取得
		File metaDir = new File(param.getMetaFilePath() + "\\triggers");
		// フィルタを作成する
		// フィルタを作成する
		FilenameFilter filter = getFilter(".trigger-meta.xml");
		File[] metaList = metaDir.listFiles(filter);

		if (metaList == null || metaList.length == 0) {
			throw new Exception("triggerメタデータが存在しません。");
		}

		for (File file : metaList) {

			// メタデータを更新するかどうかのフラグ
			Boolean blnWriteFile = false;

			// XMLを解析して
			Document document = documentBuilder.parse(file);

			Element root = document.getDocumentElement();
			NodeList objectNodeList = root.getChildNodes();

			// ApexTriggerタグの子供
			for (int i = 0; i < objectNodeList.getLength(); i++) {
				Node personNode = objectNodeList.item(i);
				if (personNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fieldElement = (Element) personNode;
					// トリガのステータスを見て、ActiveならInActiveに更新
					if (isActive == false && fieldElement.getNodeName().equals("status")
							&& "Active".equals(fieldElement.getTextContent())) {
						fieldElement.setTextContent("InActive");
						disList.triggerList.add(file.getName().substring(0, file.getName().length() - 17));
						blnWriteFile = true;
					} else if (isActive && fieldElement.getNodeName().equals("status")
							&& "InActive".equals(fieldElement.getTextContent())) {
						fieldElement.setTextContent("Active");
						disList.triggerList.add(file.getName().substring(0, file.getName().length() - 17));
						blnWriteFile = true;
					}
				}
			}
			if (blnWriteFile) {
				writeXML(file, document);
			}
		}
	}

	// 入力規則の無効化処理
	private void disableValidation(Document xmlDocument, DocumentBuilder documentBuilder, DisableList disList,
			Boolean isActive) throws Exception {

		// パスからトリガのXMLファイルリストを取得
		File metaDir = new File(param.getMetaFilePath() + "\\objects");
		// フィルタを作成する
		FilenameFilter filter = getFilter(".object");
		File[] metaList = metaDir.listFiles(filter);

		if (metaList == null || metaList.length == 0) {
			throw new Exception("オブジェクトメタデータが存在しません。");
		}

		for (File file : metaList) {

			// XMLを解析して
			Document document = documentBuilder.parse(file);

			Element root = document.getDocumentElement();
			NodeList objectNodeList = root.getChildNodes();

			// メタデータを更新するかどうかのフラグ
			Boolean blnWriteFile = false;

			String objectName = null;

			// ApexTriggerタグの子供
			for (int i = 0; i < objectNodeList.getLength(); i++) {
				Node personNode = objectNodeList.item(i);
				if (personNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fieldElement = (Element) personNode;
					// トリガのステータスを見て、ActiveならInActiveに更新
					if (fieldElement.getNodeName().equals("validationRules")) {

						String validationName = null;

						NodeList valiChildNodeList = fieldElement.getChildNodes();
						for (int j = 0; j < valiChildNodeList.getLength(); j++) {
							Node node = valiChildNodeList.item(j);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								// Element childElement = (Element) node;
								if ("fullName".equals(node.getNodeName())) {
									validationName = node.getTextContent();
								} else if (isActive == false && "active".equals(node.getNodeName())
										&& "true".equals(node.getTextContent())) {

									node.setTextContent("false");
									objectName = file.getName().substring(0, file.getName().length() - 7);
									blnWriteFile = true;
								} else if (isActive && "active".equals(node.getNodeName())
										&& "false".equals(node.getTextContent())) {

									node.setTextContent("true");
									objectName = file.getName().substring(0, file.getName().length() - 7);
									blnWriteFile = true;
								}
							}
						}
						// 有効な入力規則が存在する場合、オブジェクト名＋入力規則名を退避する。
						if (blnWriteFile) {
							disList.validationList.add(objectName + "." + validationName);
						}
					}
				}
			}
			if (blnWriteFile) {
				writeXML(file, document);
			}
		}
	}

	// 入力規則の無効化処理
	private void disableProcessBuilder(Document xmlDocument, DocumentBuilder documentBuilder, DisableList disList,
			Boolean isActive) throws Exception {

		// パスからトリガのXMLファイルリストを取得
		File metaDir = new File(param.getMetaFilePath() + "\\flowDefinitions");
		// フィルタを作成する
		FilenameFilter filter = getFilter(".flowDefinition");
		File[] metaList = metaDir.listFiles(filter);

		if (metaList == null || metaList.length == 0) {
			throw new Exception("プロセスビルダーメタデータが存在しません。");
		}

		for (File file : metaList) {

			// XMLを解析して
			Document document = documentBuilder.parse(file);

			Element root = document.getDocumentElement();
			NodeList objectNodeList = root.getChildNodes();

			// メタデータを更新するかどうかのフラグ
			Boolean blnWriteFile = false;

			// ApexTriggerタグの子供
			for (int i = 0; i < objectNodeList.getLength(); i++) {
				Node personNode = objectNodeList.item(i);
				if (personNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fieldElement = (Element) personNode;
					// プロセスビルダのactiveVersionNumberが存在し、0以外であれば0（無効）に更新
					if (isActive == false && fieldElement.getNodeName().equals("activeVersionNumber")
							&& !"0".equals(fieldElement.getTextContent())) {
						// プロセス名と有効バージョンを退避
						disList.processList.add(file.getName().substring(0, file.getName().length() - 15) + "."
								+ fieldElement.getTextContent());
						fieldElement.setTextContent("0");
						blnWriteFile = true;
					} else if (isActive && fieldElement.getNodeName().equals("activeVersionNumber")
							&& "0".equals(fieldElement.getTextContent())) {
						// プロセス名と有効バージョンを退避
						disList.processList.add(file.getName().substring(0, file.getName().length() - 15) + "."
								+ fieldElement.getTextContent());
						fieldElement.setTextContent(
								disList.processVersion.get(file.getName().substring(0, file.getName().length() - 15)));
						blnWriteFile = true;
					}
				}
			}
			if (blnWriteFile) {
				writeXML(file, document);
			}
		}

	}

	// 入力規則の無効化処理
	private void disableWorkFlow(Document xmlDocument, DocumentBuilder documentBuilder, DisableList disList,
			Boolean isActive) throws Exception {

		// パスからトリガのXMLファイルリストを取得
		File metaDir = new File(param.getMetaFilePath() + "\\workflows");
		// フィルタを作成する
		FilenameFilter filter = getFilter(".workflow");
		File[] metaList = metaDir.listFiles(filter);

		if (metaList == null || metaList.length == 0) {
			throw new Exception("ワークフローメタデータが存在しません。");
		}

		for (File file : metaList) {

			// XMLを解析して
			Document document = documentBuilder.parse(file);

			Element root = document.getDocumentElement();
			NodeList objectNodeList = root.getChildNodes();

			// メタデータを更新するかどうかのフラグ
			Boolean blnWriteFile = false;

			// ApexTriggerタグの子供
			for (int i = 0; i < objectNodeList.getLength(); i++) {
				Node personNode = objectNodeList.item(i);
				if (personNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fieldElement = (Element) personNode;
					// トリガのステータスを見て、ActiveならInActiveに更新
					if (fieldElement.getNodeName().equals("rules")) {

						String ruleName = null;

						NodeList valiChildNodeList = fieldElement.getChildNodes();
						for (int j = 0; j < valiChildNodeList.getLength(); j++) {
							Node node = valiChildNodeList.item(j);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								// Element childElement = (Element) node;
								if ("fullName".equals(node.getNodeName())) {
									ruleName = node.getTextContent();
								} else if (isActive == false && "active".equals(node.getNodeName())
										&& "true".equals(node.getTextContent())) {

									node.setTextContent("false");
									blnWriteFile = true;
								} else if (isActive && "active".equals(node.getNodeName())
										&& "false".equals(node.getTextContent())) {

									node.setTextContent("true");
									blnWriteFile = true;
								}
							}
						}
						// 有効な入力規則が存在する場合、オブジェクト名＋入力規則名を退避する。
						if (blnWriteFile) {
							disList.workFlowList
									.add(file.getName().substring(0, file.getName().length() - 9) + "." + ruleName);
						}
					}
				}
			}
			if (blnWriteFile) {
				writeXML(file, document);
			}
		}
	}

	private void writeXML(File file, Document document) throws Exception {

		Transformer transformer = null;
		try {
			document.setXmlStandalone(true);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformer = transformerFactory.newTransformer();
		} catch (Exception e) {
			throw e;
		}
		transformer.setOutputProperty("indent", "yes");
		transformer.setOutputProperty("encoding", "UTF-8");

		try {
			transformer.transform(new DOMSource(document), new StreamResult(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void exportCSV(DisableList disList) {
		try {

			// 出力ファイルの作成
			FileWriter f = new FileWriter(param.getMetaFilePath() + "\\success.csv", false);
			PrintWriter p = new PrintWriter(new BufferedWriter(f));

			// ヘッダーを指定する
			p.print("コンポーネント");
			p.print(",");
			p.print("名前");
			p.println();

			// 内容をセットする
			if (disList.triggerList.size() > 0) {
				for (String triggerName : disList.triggerList) {
					p.print("ApexTrigger");
					p.print(",");
					p.print(triggerName);
					p.println(); // 改行
				}
			}

			if (disList.validationList.size() > 0) {
				for (String validationName : disList.validationList) {
					p.print("ValidationRule");
					p.print(",");
					p.print(validationName);
					p.println(); // 改行
				}
			}

			if (disList.processList.size() > 0) {
				for (String processBuilderName : disList.processList) {
					p.print("ProcessBuilder");
					p.print(",");
					p.print(processBuilderName);
					p.println(); // 改行
				}
			}

			if (disList.workFlowList.size() > 0) {
				for (String workFlowName : disList.workFlowList) {
					p.print("WorkFlow");
					p.print(",");
					p.print(workFlowName);
					p.println(); // 改行
				}
			}

			// ファイルに書き出し閉じる
			p.close();

			System.out.println("ファイル出力完了！");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// ファイルのフィルタを取得。拡張子を指定する。
	private FilenameFilter getFilter(String filterTxt) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File file, String str) {
				if (str.endsWith(filterTxt)) {
					return true;
				} else {
					return false;
				}
			}
		};
		return filter;
	}

	public String getSuffix(String fileName) {
		if (fileName == null)
			return null;
		int point = fileName.lastIndexOf(".");
		if (point != -1) {
			return fileName.substring(point + 1);
		}
		return fileName;
	}

	private void setProcessVersion(DisableList disList) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(param.getDisableFileList()));

		String line;
		// 1行ずつCSVファイルを読み込む
		while ((line = br.readLine()) != null) {
			String[] data = line.split(",", 0); // 行をカンマ区切りで配列に変換
			// プロセスビルダなら、名前とバージョンを格納
			if ("ProcessBuilder".equals(data[0])) {
				// プロセスビルダー名をキーに、バージョンをマップにつめる
				String processName = data[1];
				disList.processVersion.put(processName.substring(0, processName.lastIndexOf('.')),
						getSuffix(processName));
			}
		}
		br.close();
	}
}
