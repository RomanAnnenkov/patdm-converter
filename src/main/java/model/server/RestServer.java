package model.server;

import com.itextpdf.text.DocumentException;
import exceptions.ImageIsNotPNGExceptions;
import exceptions.MosaicIsTooBigException;
import io.PathGenerator;
import io.image.ImageLoader;
import io.image.ImagePreserver;
import io.pdf.PDFMapPreserver;
import io.pdf.PDFPalettePreserver;
import model.converter.*;
import model.elementColorsLoader.DMCElementColors;
import model.elementColorsLoader.IMosaicElementColorLoader;
import model.image.Image;
import model.palette.Palette;
import model.server.request.RequestFile;
import model.tableMap.AlphabeticSymbols;
import model.tableMap.ITableMap;
import model.tableMap.TableMap;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableAutoConfiguration
public class RestServer {

    @GetMapping("/health-check")
    public HttpStatus healthCheck() {
        return HttpStatus.OK;
    }

    @PostMapping(value = "/process-file", headers = "content-type=application/json")
    public Map<String, String> processPng(@RequestBody RequestFile file) throws ImageIsNotPNGExceptions, IOException, DocumentException, MosaicIsTooBigException {
        String filePath = file.getFilePath();
        Map<String,String> responseFiles = new HashMap<>();

        ImageLoader loader = new ImageLoader();
        Image image = loader.loadImage(filePath);

        IElementToPixel converterSize = new ImageToElements();
        Image resizedImage = converterSize.decrease(image);
        ImagePreserver imagePreserver = new ImagePreserver();
        PathGenerator pathGenerator = new PathGenerator(filePath);
        imagePreserver.saveImage(resizedImage, pathGenerator.getPath("resized", "png"));
        responseFiles.put("resized", pathGenerator.getPath("resized", "png"));

        IMosaicElementColorLoader mosaicElementColorLoader = DMCElementColors.getInstance();
        IImagePixelColorsToElementColors colorConverter = new ImagePixelColorsToElementColors(new PixelColorToElementColor(), mosaicElementColorLoader);
        colorConverter.convertPixelColorsToElementColors(resizedImage);

        Palette palette = new Palette(new AlphabeticSymbols(), resizedImage);

        imagePreserver.saveImage(palette.generatePreview(), pathGenerator.getPath("preview", "png"));
        responseFiles.put("preview", pathGenerator.getPath("preview", "png"));

        PDFPalettePreserver pdfPalettePreserver = new PDFPalettePreserver(palette);
        pdfPalettePreserver.savePDF(pathGenerator.getPath("palette", "pdf"));
        responseFiles.put("palette", pathGenerator.getPath("palette", "pdf"));

        ITableMap tableMap = new TableMap(palette);
        String[][] forPrint = tableMap.getTableMap();

        PDFMapPreserver pdfMapPreserver = new PDFMapPreserver(forPrint);
        pdfMapPreserver.savePDF(pathGenerator.getPath("map", "pdf"));
        responseFiles.put("map", pathGenerator.getPath("map", "pdf"));

        return responseFiles;
    }

}