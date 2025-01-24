package org.entermediadb.asset.generators;

import java.io.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.filesystem.FileItem;

public class CssGenerator extends TempFileGenerator {
    private static Log log = LogFactory.getLog(CssGenerator.class);
    private PageManager pageManager;
    private Map<String, Long> cachedSizeCounts = new HashMap<>();

    public PageManager getPageManager() {
        return pageManager;
    }

    public void setPageManager(PageManager inPageManager) {
        pageManager = inPageManager;
    }

    @Override
    public void generate(WebPageRequest inContext, Page inPage, Output inOut) {
        HttpServletResponse res = inContext.getResponse();
        HttpServletRequest req = inContext.getRequest();

        String appId = inPage.get("applicationid");
        Page rootPage = getPageManager().getPage("/" + appId + "/", false);
        List<String> cssPaths = getPageManager().getStylePathsForApp(rootPage);

        if (cssPaths == null || cssPaths.isEmpty()) {
            log.warn("No CSS paths found for app: " + appId);
            return;
        }

        long mostRecentMod = 0;
        long totalSize = 0;

        for (String path : cssPaths) {
            Page file = getPageManager().getPage(path);
            if (file.exists()) {
                totalSize += file.length();
                if (file.lastModified() > mostRecentMod) {
                    mostRecentMod = file.lastModified();
                }
            }
        }

        // Check for Conditional Request (304 Not Modified)
        String ifModifiedSince = req.getHeader("If-Modified-Since");
        if (ifModifiedSince != null) {
            try {
                Date old = getLastModFormat().parse(ifModifiedSince);
                if (mostRecentMod <= old.getTime()) {
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            } catch (Exception e) {
                log.error("Invalid If-Modified-Since header: " + ifModifiedSince, e);
            }
        }

        // Check if we need to regenerate
        Long cachedTotal = cachedSizeCounts.get(inPage.getPath());
        if (cachedTotal == null || cachedTotal != totalSize || inPage.getLastModified().getTime() != mostRecentMod) {
            try {
                saveLocally(cssPaths, inPage, inOut, mostRecentMod);
                cachedSizeCounts.put(inPage.getPath(), totalSize);
            } catch (IOException e) {
                log.error("Error generating CSS", e);
            }
        }
        try
		{
			serveCss(inPage, mostRecentMod, inOut, res);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
            log.error("Error serving CSS", e);

		}


    }

    protected void saveLocally(List<String> cssPaths, Page inPage, Output inOut, long mostRecentMod) throws IOException {
        synchronized (inPage) {
            Page tempFile = pageManager.getPage(inPage.getContentItem().getAbsolutePath() + ".tmp.css");
            Writer writer = new OutputStreamWriter(tempFile.getContentItem().getOutputStream(), inPage.getCharacterEncoding());

            for (String path : cssPaths) {
                Page cssFile = pageManager.getPage(path);
                if (cssFile.exists()) {
                    try (Reader reader = new InputStreamReader(cssFile.getInputStream(), cssFile.getCharacterEncoding())) {
                        writer.write("/* CSS Source: " + path + " */\n");
                        getOutputFiller().fill(reader, writer);
                        writer.write("\n");
                    }
                } else {
                    writer.write("/* CSS File Not Found: " + path + " */\n");
                }
            }

            writer.close();
            pageManager.removePage(inPage);
            pageManager.movePage(tempFile, inPage);

            if (inPage.getContentItem() instanceof FileItem) {
                FileItem fileItem = (FileItem) inPage.getContentItem();
                fileItem.getFile().setLastModified(mostRecentMod);
            }
        }
    }

    protected void serveCss(Page inPage, long mostRecentMod, Output inOut, HttpServletResponse res) throws IOException {
        res.setContentType("text/css");
        res.setDateHeader("Last-Modified", mostRecentMod);

        try (Reader reader = new InputStreamReader(inPage.getInputStream(), inPage.getCharacterEncoding())) {
            getOutputFiller().fill(reader, inOut.getWriter());
        }
    }
}
