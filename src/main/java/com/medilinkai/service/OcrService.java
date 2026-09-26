package com.medilinkai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Extracts text from a prescription image.
 *
 * First tries real OCR with Tesseract (via Tess4J). If Tesseract or its
 * tessdata folder is missing (common on demo machines), it falls back to a
 * simulated result so the feature is always demoable: the file name is used
 * as the "text", and known medicine names inside it are matched later.
 */
@Service
public class OcrService {

    @Value("${medilink.tessdata}")
    private String tessdataPath;

    /**
     * @param imageFile the saved prescription image
     * @param originalFilename the uploaded file's original name (fallback input)
     * @return extracted text, never null
     */
    public String extractText(File imageFile, String originalFilename) {
        if (tesseractAvailable()) {
            try {
                net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
                tesseract.setDatapath(tessdataPath);
                tesseract.setLanguage("eng");
                String text = tesseract.doOCR(imageFile);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            } catch (Throwable t) {
                // fall through to simulated OCR
            }
        }
        return simulatedOcr(originalFilename);
    }

    private boolean tesseractAvailable() {
        try {
            File tessdata = new File(tessdataPath);
            return tessdata.isDirectory()
                    && new File(tessdata, "eng.traineddata").isFile();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Simulated OCR: turns a file name like "rx_napa_seclo.jpg" into the
     * readable text "rx napa seclo", which the medicine matcher then uses.
     */
    private String simulatedOcr(String filename) {
        String base = filename == null ? "" : filename;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.replace('_', ' ').replace('-', ' ').trim();
    }
}
