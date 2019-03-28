package jp.co.ncast.form;

import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

public class CustomFileFilter extends FileFilter {

	ArrayList<String> fileTypeList;

	public CustomFileFilter(ArrayList<String> fileTypeList) throws Exception {
		if (fileTypeList == null || fileTypeList.size() == 0) {
			throw new Exception("プログラムがバグってます。。すいません。");
		}
		this.fileTypeList = fileTypeList;
	}

	public boolean accept(File f) {
		/* ディレクトリなら無条件で表示する */
		if (f.isDirectory()) {
			return true;
		}

		/* 拡張子を取り出し、html又はhtmだったら表示する */
		String ext = getExtension(f);
		if (ext != null) {
			if (fileTypeList.contains(ext)) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	public String getDescription() {
		return "HTMLファイル";
	}

	/* 拡張子を取り出す */
	private String getExtension(File f) {
		String ext = null;
		String filename = f.getName();
		int dotIndex = filename.lastIndexOf('.');

		if ((dotIndex > 0) && (dotIndex < filename.length() - 1)) {
			ext = filename.substring(dotIndex + 1).toLowerCase();
		}

		return ext;
	}
}