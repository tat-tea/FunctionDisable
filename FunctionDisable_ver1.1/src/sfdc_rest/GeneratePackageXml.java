package sfdc_rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GeneratePackageXml {

	static final String USERNAME = "s.ueno@pmo.co.jp.test";
	static final String PASSWORD = "pmo12345";
	static final String LOGINURL = "https://test.salesforce.com";
	static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
	static final String CLIENTID = "3MVG9e2mBbZnmM6kZ.oxNQY5VpGv0.PxXwKUavcOGKqkXPdu7_NTFiLejWllP8Tl62F3zYBg9nY8DYQk6c4Mt";
	static final String CLIENTSECRET = "910CF88D0B946EEBEEEB0D6965B25EC751C706104286C7FA977F240753C28BFE";
	static final String TOOLING_API_PATH = "/services/data/v44.0/tooling/query/";

	// ログインインスタンスURL
	private String loginInstanceUrl = null;
	private String loginAccessToken = null;
	private HttpPost httpPost;

	private static final String getActiveTrigger = "select id, name, status from ApexTrigger where status = 'Active'";
	private static final String getActiveValidation = "SELECT ValidationName, Active, EntityDefinition.QualifiedApiName FROM ValidationRule WHERE Active = true ";
	private static final String getActiveProcessBuilder = "select DeveloperName, ActiveVersion.VersionNumber, ActiveVersion.Status from FlowDefinition where ActiveVersion.Status = 'Active'";
	private static final String getActiveWorkflowRule = "SELECT Id, Name, TableEnumOrId FROM WorkflowRule";

	// コンストラクタ
	public GeneratePackageXml() throws Exception {
		// 認証処理
		authorization();
	}

	public void generateXml() throws Exception {

		// 有効なトリガー一覧取得
		ArrayList<String> triggerList = getActiveTrigger();
		ArrayList<String> validationList = getValidationRule();
		ArrayList<String> processList = getProcessBuilder();
		ArrayList<String> workflowList = getWorkFloｗRule();

		// release connection
		httpPost.releaseConnection();

		// packageXMLを作成する。
		// ドキュメントビルダーファクトリを生成
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// ドキュメントビルダーを生成
		DocumentBuilder builder = factory.newDocumentBuilder();
		// Documentオブジェクトを取得
		Document xmlDocument = builder.newDocument();

		// xmlDocument.setXmlStandalone(true);

		// XML作成
		File newXML = new File("C:\\Users\\CAST-N172\\Desktop\\ツール検証\\package.xml");
		FileOutputStream fos = new FileOutputStream(newXML);
		StreamResult result = new StreamResult(fos);

		Element Package = xmlDocument.createElement("Package");
		Package.setAttribute("Package", "http://soap.sforce.com/2006/04/metadata");
		xmlDocument.appendChild(Package);

		// XMLに書いていく
		writeXML(xmlDocument, Package, triggerList, "ApexTrigger");
		writeXML(xmlDocument, Package, validationList, "ValidationRule");
		writeXML(xmlDocument, Package, processList, "Flow");
		writeXML(xmlDocument, Package, workflowList, "WorkflowRule");

		// 最後にバージョンを追記
		Element version = xmlDocument.createElement("version");
		version.appendChild(xmlDocument.createTextNode("44.0"));
		Package.appendChild(version);

		// Transformerファクトリを生成
		TransformerFactory transFactory = TransformerFactory.newInstance();
		// Transformerを取得
		Transformer transformer = transFactory.newTransformer();

		// エンコード：UTF-8、インデントありを指定
		transformer.setOutputProperty("encoding", "UTF-8");
		transformer.setOutputProperty("indent", "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		// transformerに渡すソースを生成
		DOMSource source = new DOMSource(xmlDocument);

		// 出力実行
		transformer.transform(source, result);
		fos.close();
	}

	// 認証処理
	private void authorization() throws Exception {
		// 認証処理
		DefaultHttpClient httpclient = new DefaultHttpClient();

		// Assemble the login request URL
		String loginURL = LOGINURL + GRANTSERVICE + "&client_id=" + CLIENTID + "&client_secret=" + CLIENTSECRET
				+ "&username=" + USERNAME + "&password=" + PASSWORD;

		// Login requests must be POSTs
		httpPost = new HttpPost(loginURL);
		HttpResponse response = null;

		try {
			// Execute the login POST request
			response = httpclient.execute(httpPost);
		} catch (ClientProtocolException cpException) {
			// Handle protocol exception
		} catch (IOException ioException) {
			// Handle system IO exception
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			System.out.println("Error authenticating to Force.com: " + statusCode);
			// Error is in EntityUtils.toString(response.getEntity())
			throw new Exception("認証に失敗しました。");
		}

		String getResult = null;
		getResult = EntityUtils.toString(response.getEntity());
		JSONObject jsonObject = null;

		jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
		loginAccessToken = jsonObject.getString("access_token");
		loginInstanceUrl = jsonObject.getString("instance_url");
		System.out.println(response.getStatusLine());
		System.out.println("Successful login");
		System.out.println("  instance URL: " + loginInstanceUrl);
		System.out.println("  access token/session ID: " + loginAccessToken);
	}

	// 組織上で有効なトリガーを取得する
	private ArrayList<String> getActiveTrigger() throws Exception {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		JSONObject jsonObject = null;
		URIBuilder builder = new URIBuilder(loginInstanceUrl);
		builder.setPath(TOOLING_API_PATH).setParameter("q", getActiveTrigger);
		HttpGet get = new HttpGet(builder.build());
		get.setHeader("Authorization", "Bearer " + loginAccessToken);

		HttpResponse queryResponse = httpclient.execute(get);

		// HTTPレスポンスコードを確認する
		int statusCode2 = queryResponse.getStatusLine().getStatusCode();
		if (statusCode2 != HttpStatus.SC_OK) {
			System.out.println("Tooling Api Request Select ApexTrigger:" + statusCode2);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult2 = null;
		getResult2 = EntityUtils.toString(queryResponse.getEntity());
		jsonObject = (JSONObject) new JSONTokener(getResult2).nextValue();
		JSONArray array = jsonObject.getJSONArray("records");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject triggers = (JSONObject) array.get(i);
			result.add(triggers.getString("Name"));
		}

		return result;
	}

	// 組織上で有効な入力規則を取得する
	private ArrayList<String> getValidationRule() throws Exception {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		JSONObject jsonObject = null;
		URIBuilder builder = new URIBuilder(loginInstanceUrl);
		builder.setPath(TOOLING_API_PATH).setParameter("q", getActiveValidation);
		HttpGet get = new HttpGet(builder.build());
		get.setHeader("Authorization", "Bearer " + loginAccessToken);

		HttpResponse queryResponse = httpclient.execute(get);

		// HTTPレスポンスコードを確認する
		int statusCode2 = queryResponse.getStatusLine().getStatusCode();
		if (statusCode2 != HttpStatus.SC_OK) {
			System.out.println("Tooling Api Request Select ValidationRule:" + statusCode2);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult2 = null;
		getResult2 = EntityUtils.toString(queryResponse.getEntity());
		jsonObject = (JSONObject) new JSONTokener(getResult2).nextValue();
		JSONArray array = jsonObject.getJSONArray("records");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject triggers = (JSONObject) array.get(i);
			String validationName = triggers.getString("ValidationName");
			JSONObject entityDefinition = triggers.getJSONObject("EntityDefinition");
			result.add(entityDefinition.getString("QualifiedApiName") + "." + validationName);
		}
		return result;
	}

	// 組織上で有効なプロセスビルダーを取得する
	private ArrayList<String> getProcessBuilder() throws Exception {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		JSONObject jsonObject = null;
		URIBuilder builder = new URIBuilder(loginInstanceUrl);
		builder.setPath(TOOLING_API_PATH).setParameter("q", getActiveProcessBuilder);
		HttpGet get = new HttpGet(builder.build());
		get.setHeader("Authorization", "Bearer " + loginAccessToken);

		HttpResponse queryResponse = httpclient.execute(get);

		// HTTPレスポンスコードを確認する
		int statusCode2 = queryResponse.getStatusLine().getStatusCode();
		if (statusCode2 != HttpStatus.SC_OK) {
			System.out.println("Tooling Api Request Select ValidationRule:" + statusCode2);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult2 = null;
		getResult2 = EntityUtils.toString(queryResponse.getEntity());
		jsonObject = (JSONObject) new JSONTokener(getResult2).nextValue();
		JSONArray array = jsonObject.getJSONArray("records");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject process = (JSONObject) array.get(i);
			result.add(process.getString("DeveloperName"));
		}
		return result;
	}

	// 組織上で有効なワークフローを取得する
	private ArrayList<String> getWorkFloｗRule() throws Exception {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		JSONObject jsonObject = null;
		URIBuilder builder = new URIBuilder(loginInstanceUrl);
		builder.setPath(TOOLING_API_PATH).setParameter("q", getActiveWorkflowRule);
		HttpGet get = new HttpGet(builder.build());
		get.setHeader("Authorization", "Bearer " + loginAccessToken);

		HttpResponse queryResponse = httpclient.execute(get);

		// HTTPレスポンスコードを確認する
		int statusCode2 = queryResponse.getStatusLine().getStatusCode();
		if (statusCode2 != HttpStatus.SC_OK) {
			System.out.println("Tooling Api Request Select ValidationRule:" + statusCode2);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult2 = null;
		getResult2 = EntityUtils.toString(queryResponse.getEntity());
		jsonObject = (JSONObject) new JSONTokener(getResult2).nextValue();
		JSONArray array = jsonObject.getJSONArray("records");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject process = (JSONObject) array.get(i);
			result.add(process.getString("TableEnumOrId") + "." + process.getString("Name"));
		}
		System.out.println("work" + result);
		return result;
	}

	public Document createXMLDocument(String root) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		DOMImplementation dom = builder.getDOMImplementation();
		return dom.createDocument("", root, null);
	}

	// メソッド定義
	public String createXMLString(Document document) throws TransformerException {
		StringWriter writer = new StringWriter();
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");

		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return writer.toString();
	}

	private void writeXML(Document xmlDocument, Element Package, ArrayList<String> memberList, String Name) {
		if (memberList != null && memberList.size() > 0) {
			Element types = xmlDocument.createElement("types");
			Package.appendChild(types);
			for (String member : memberList) {
				Element members = xmlDocument.createElement("members");
				members.appendChild(xmlDocument.createTextNode(member));
				types.appendChild(members);
			}
			Element name = xmlDocument.createElement("name");
			name.appendChild(xmlDocument.createTextNode(Name));
			types.appendChild(name);
		}
	}
}