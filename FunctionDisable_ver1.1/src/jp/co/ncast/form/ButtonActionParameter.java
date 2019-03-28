package jp.co.ncast.form;

import java.io.File;

public class ButtonActionParameter {

	private String metaFilePath = null;
	private boolean chckbx_Validation = false;
	private boolean chckbx_Trigger = false;
	private boolean chckbx_Process = false;
	private boolean chckbx_Workflow = false;
	private File disableFileList = null;

	public File getDisableFileList() {
		return disableFileList;
	}

	public void setDisableFileList(File disableFileList) {
		this.disableFileList = disableFileList;
	}

	public String getMetaFilePath() {
		return metaFilePath;
	}

	public void setMetaFilePath(String metaFilePath) {
		this.metaFilePath = metaFilePath;
	}

	public boolean isChckbx_Validation() {
		return chckbx_Validation;
	}

	public void setChckbx_Validation(boolean chckbx_Validation) {
		this.chckbx_Validation = chckbx_Validation;
	}

	public boolean isChckbx_Trigger() {
		return chckbx_Trigger;
	}

	public void setChckbx_Trigger(boolean chckbx_Trigger) {
		this.chckbx_Trigger = chckbx_Trigger;
	}

	public boolean isChckbx_Process() {
		return chckbx_Process;
	}

	public void setChckbx_Process(boolean chckbx_Process) {
		this.chckbx_Process = chckbx_Process;
	}

	public boolean isChckbx_Workflow() {
		return chckbx_Workflow;
	}

	public void setChckbx_Workflow(boolean chckbx_Workflow) {
		this.chckbx_Workflow = chckbx_Workflow;
	}

}
