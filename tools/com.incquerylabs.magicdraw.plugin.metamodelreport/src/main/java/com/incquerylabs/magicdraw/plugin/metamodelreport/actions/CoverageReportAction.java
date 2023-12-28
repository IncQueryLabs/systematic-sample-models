package com.incquerylabs.magicdraw.plugin.metamodelreport.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;

import javax.annotation.CheckForNull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public abstract class CoverageReportAction extends MDAction {
	private static final long serialVersionUID = 1L;
	protected static final String TEMPLATE__CAT_ID = "%S_COVERAGE_REPORT_GENERATOR";
	protected static final String TEMPLATE__CAT_NAME = "Generate %s Coverage Report";
	protected final String type;
	private static File baseDir = null;

	@Override
	public final void actionPerformed(@CheckForNull ActionEvent actionEvent) {
		// select output directory
		JFileChooser outputDirectorySelector = new JFileChooser();
		outputDirectorySelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		outputDirectorySelector.setDialogTitle("Output Directory Selector");
		outputDirectorySelector.setCurrentDirectory(baseDir);
		if(outputDirectorySelector.showOpenDialog(Application.getInstance().getMainFrame()) == JFileChooser.APPROVE_OPTION) {
			baseDir = outputDirectorySelector.getSelectedFile();
		} else {
			return;
		}

		// execute the report generation
		Result result = actionPerformed();

		// user feedback
		GUILog log = Application.getInstance().getGUILog();
		if(result.succeeded) {
			log.log(result.message);
		} else if(result.error != null) {
			log.showError(result.message, result.error);
		} else {
			log.showError(result.message);
		}
	}

	protected File getBaseDir() {
		return baseDir;
	}

	public abstract Result actionPerformed();

	protected CoverageReportAction(String type) {
		super(String.format(TEMPLATE__CAT_ID, type), String.format(TEMPLATE__CAT_NAME, type), null, null);
		this.type = type.toLowerCase();
	}
	protected final void serializeToJson(Object coverageDTO, File outputDir) {
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		File outputFile = new File(outputDir, String.format("%sCoverageInfo.json", type));
		try (FileWriter writer = new FileWriter(outputFile)) {
			gson.toJson(coverageDTO, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected final File setupDestinationDirectory(Project project) {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
		String projectName = project.getName();

		return new File(Paths.get(
				getBaseDir() != null
						? getBaseDir().getAbsolutePath()
						: "./ModelCoverageInfo",
				projectName,
				simpleDate.format(timestamp)).toString());
	}

	protected static final class Result {
		public final boolean succeeded;
		public final String message;
		public final Throwable error;

		private static final String OUTPUT_TEMPLATE = "Report files were successfully generated into the following directory: %s";

		public Result(File outputDir) {
			this.succeeded = true;
			this.message = String.format(OUTPUT_TEMPLATE, outputDir.toString());
			this.error = null;
		}
		public Result(String message, Throwable error) {
			this.succeeded = true;
			this.message = message;
			this.error = error;
		}
	}
}
