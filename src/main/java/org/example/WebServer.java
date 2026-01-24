import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Importăm biblioteca iText pentru generarea fișierului PDF
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.*;

public class WebServer {

    private static final String ORAR_FILE = "orar.json";
    // Creăm un Gson cu pretty printing (JSON frumos formatat)
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Orarul va fi păstrat în format JSON în fisier
    private static Map<String, List<OrarEntry>> ORARE = new HashMap<>();

    static {
        // Încarcăm orarul din JSON sau, dacă nu există, îl creăm și salvăm
        File file = new File(ORAR_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, List<OrarEntry>>>(){}.getType();
                ORARE = gson.fromJson(reader, type);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            initOrar();        // creăm orarul hardcodat
            saveOrarToFile();  // salvăm în JSON
        }
    }

    private static void initOrar() {
        // Grupa CR-221FR
        List<OrarEntry> orarCR221FR = new ArrayList<>();
        orarCR221FR.add(new OrarEntry("Luni", "2025-09-15", "08:00-13:00", "Curs", "Testare Software", "Cărbune V.", "Auditoriu 722"));
        orarCR221FR.add(new OrarEntry("Marți", "2025-09-16", "08:00-13:00", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 505"));
        orarCR221FR.add(new OrarEntry("Marți", "2025-09-16", "13:30-18:30", "Curs", "Programare concurentă și distribuită", "Rotaru L.", "Auditoriu 619"));
        orarCR221FR.add(new OrarEntry("Miercuri", "2025-09-17", "08:00-11:15", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 402"));
        orarCR221FR.add(new OrarEntry("Miercuri", "2025-09-17", "11:30-13:00", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 722"));
        orarCR221FR.add(new OrarEntry("Miercuri", "2025-09-17", "13:30-18:30", "Curs", "Sisteme de operare", "Rotaru L.", "Auditoriu 720"));
        orarCR221FR.add(new OrarEntry("Joi", "2025-09-18", "08:00-11:15", "Curs", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 402"));
        orarCR221FR.add(new OrarEntry("Joi", "2025-09-18", "11:30-13:00", "Curs", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 224"));
        orarCR221FR.add(new OrarEntry("Joi", "2025-09-18", "13:30-18:30", "Seminar", "Sisteme de operare", "Kapusteanschi M.", "Auditoriu 217"));
        orarCR221FR.add(new OrarEntry("Vineri", "2025-09-19", "08:00-11:15", "Curs", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 201"));
        orarCR221FR.add(new OrarEntry("Vineri", "2025-09-19", "11:30-13:00", "Curs", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 709"));
        orarCR221FR.add(new OrarEntry("Vineri", "2025-09-19", "13:30-18:30", "Laborator", "Testare Software", "Cărbune V.", "Auditoriu 215"));
        orarCR221FR.add(new OrarEntry("Sâmbătă", "2025-09-20", "08:00-13:00", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 310"));
        orarCR221FR.add(new OrarEntry("Sâmbătă", "2025-09-20", "13:30-18:30", "Laborator", "Aplicație Client-Server", "Rotaru L.", "Auditoriu 217"));
        orarCR221FR.add(new OrarEntry("Duminică", "2025-09-21", "08:00-13:00", "Laborator", "Aplicație Client-Server", "Rotaru L.", "Auditoriu 217"));
        orarCR221FR.add(new OrarEntry("Duminică", "2025-09-21", "13:30-18:30", "Laborator", "Programare concurentă și distribuită", "Kapusteanschi M.", "Auditoriu 217"));
        orarCR221FR.add(new OrarEntry("Luni", "2025-09-22", "08:00-13:00", "Curs", "Sisteme de operare", "Rotaru L.", "Auditoriu 718"));
        orarCR221FR.add(new OrarEntry("Luni", "2025-09-22", "13:30-15:00", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 609"));
        orarCR221FR.add(new OrarEntry("Luni", "2025-09-22", "15:15-18:30", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 405"));
        orarCR221FR.add(new OrarEntry("Marți", "2025-09-23", "08:00-13:00", "Curs", "Programare concurentă și distribuită", "Rotaru L.", "Auditoriu 515"));
        orarCR221FR.add(new OrarEntry("Marți", "2025-09-23", "13:30-18:30", "Curs", "Testare software", "Cărbune V.", "Auditoriu 604"));
        orarCR221FR.add(new OrarEntry("Miercuri", "2025-09-24", "08:00-15:00", "Seminar", "Programare concurentă și distribuită", "Rotaru L.", "Auditoriu 709"));
        orarCR221FR.add(new OrarEntry("Miercuri", "2025-09-24", "15:15-18:30", "Laborator", "Programare concurentă și distribuită", "Kapusteanschi M.", "Auditoriu 215"));
        orarCR221FR.add(new OrarEntry("Joi", "2025-09-25", "08:00-13:00", "Seminar", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 011"));
        orarCR221FR.add(new OrarEntry("Joi", "2025-09-25", "13:30-18:30", "Seminar", "Sisteme de operare", "Kapusteanschi M.", "Auditoriu 217"));
        orarCR221FR.add(new OrarEntry("Vineri", "2025-09-26", "15:15-20:15", "Laborator", "Programare de sistem și de rețea", "Moraru V.", "Auditoriu 215"));
        orarCR221FR.add(new OrarEntry("Sâmbătă", "2025-09-27", "08:00-13:00", "Laborator", "Testare software", "Cărbune V.", "Auditoriu 215"));
        ORARE.put("CR-221FR", orarCR221FR);

        // Grupa TI-221FR
        List<OrarEntry> orarTI221FR = new ArrayList<>();
        orarTI221FR.add(new OrarEntry("Luni", "2025-09-15", "08:00-13:00", "Curs", "Sisteme de operare", "Colesnic V.", "Auditoriu 718"));
        orarTI221FR.add(new OrarEntry("Marți", "2025-09-16", "08:00-13:00", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 505"));
        orarTI221FR.add(new OrarEntry("Marți", "2025-09-16", "13:30-18:30", "Curs", "Sisteme de operare", "Colesnic V.", "Auditoriu 107"));
        orarTI221FR.add(new OrarEntry("Miercuri", "2025-09-17", "08:00-11:15", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 402"));
        orarTI221FR.add(new OrarEntry("Miercuri", "2025-09-17", "11:30-13:00", "Curs", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 722"));
        orarTI221FR.add(new OrarEntry("Miercuri", "2025-09-17", "13:30-18:30", "Curs", "Baze de date", "Saranciuc D.", "Auditoriu 609"));
        orarTI221FR.add(new OrarEntry("Joi", "2025-09-18", "09:45-13:00", "Curs", "Baze de date", "Saranciuc D.", "Auditoriu 720"));
        orarTI221FR.add(new OrarEntry("Joi", "2025-09-18", "13:30-18:30", "Curs", "Inteligență artificială", "Rusu M.", "Auditoriu 107"));
        orarTI221FR.add(new OrarEntry("Vineri", "2025-09-19", "09:45-13:00", "Curs", "Sisteme de operare", "Colesnic V.", "Auditoriu 524"));
        orarTI221FR.add(new OrarEntry("Vineri", "2025-09-19", "13:30-18:30", "Curs", "Inteligență artificială", "Rusu M.", "Auditoriu 404"));
        orarTI221FR.add(new OrarEntry("Sâmbătă", "2025-09-20", "08:00-13:00", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 310"));
        orarTI221FR.add(new OrarEntry("Sâmbătă", "2025-09-20", "13:30-18:30", "Laborator", "Baze de date", "Saranciuc D.", "Auditoriu 502"));
        orarTI221FR.add(new OrarEntry("Duminică", "2025-09-21", "08:00-15:00", "Laborator", "Sisteme de operare", "Colesnic V.", "Auditoriu 628"));
        orarTI221FR.add(new OrarEntry("Duminică", "2025-09-21", "15:15-18:30", "Laborator", "Baze de date", "Saranciuc D.", "Auditoriu 628"));
        orarTI221FR.add(new OrarEntry("Luni", "2025-09-22", "08:00-13:00", "Curs", "Testarea produselor program", "Prisăcaru A.", "Auditoriu 630"));
        orarTI221FR.add(new OrarEntry("Luni", "2025-09-22", "13:30-15:00", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 609"));
        orarTI221FR.add(new OrarEntry("Luni", "2025-09-22", "15:15-18:30", "Seminar", "Antreprenoriat", "Gumeniuc I.", "Auditoriu 405"));
        orarTI221FR.add(new OrarEntry("Marți", "2025-09-23", "13:30-18:30", "Laborator", "Sisteme de operare", "Colesnic V.", "Auditoriu 503"));
        orarTI221FR.add(new OrarEntry("Miercuri", "2025-09-24", "08:00-13:00", "Curs", "Testarea produselor program", "Prisăcaru A.", "Auditoriu 609"));
        orarTI221FR.add(new OrarEntry("Miercuri", "2025-09-24", "13:30-16:45", "Seminar", "Baze de date", "Saranciuc D.", "Auditoriu 720"));
        orarTI221FR.add(new OrarEntry("Joi", "2025-09-25", "08:00-13:00", "Seminar", "Testarea produselor program", "Prisăcaru A.", "Auditoriu 516"));
        orarTI221FR.add(new OrarEntry("Joi", "2025-09-25", "13:30-16:45", "Laborator", "Baze de date", "Saranciuc D.", "Auditoriu 613"));
        orarTI221FR.add(new OrarEntry("Joi", "2025-09-25", "17:00-18:30", "Seminar", "Baze de date", "Saranciuc D.", "Auditoriu 501"));
        orarTI221FR.add(new OrarEntry("Vineri", "2025-09-26", "13:30-18:30", "Laborator", "Inteligență artificială", "Rusu M.", "Auditoriu 518"));
        orarTI221FR.add(new OrarEntry("Sâmbătă", "2025-09-27", "08:00-13:00", "Seminar", "Testarea produselor program", "Prisăcaru A.", "Auditoriu 516"));
        orarTI221FR.add(new OrarEntry("Sâmbătă", "2025-09-27", "13:30-18:30", "Laborator", "Inteligență artificială", "Rusu M.", "Auditoriu 502"));
        ORARE.put("TI-221FR", orarTI221FR);
    }

    private static void saveOrarToFile() {
        try (Writer writer = new FileWriter(ORAR_FILE)) {
            gson.toJson(ORARE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clasa pentru o intrare în orar
    static class OrarEntry {
        String ziua, data, ora, tip, disciplina, profesor, sala;

        OrarEntry(String ziua, String data, String ora, String tip, String disciplina, String profesor, String sala) {
            this.ziua = ziua;
            this.data = data;
            this.ora = ora;
            this.tip = tip;
            this.disciplina = disciplina;
            this.profesor = profesor;
            this.sala = sala;
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        System.out.println("Server HTTP pornit pe http://localhost:8080");
        server.createContext("/", new OrarHandler());
        server.createContext("/export-pdf", new PdfHandler());
        server.setExecutor(null);
        server.start();
    }

    static class OrarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String grupa = null, data = null, profesor = null;

            if (query != null) {
                Map<String, String> params = WebServer.parseQuery(query);
                grupa = params.get("grupa");
                data = params.get("data");
                profesor = params.get("profesor");
            }

            StringBuilder response = new StringBuilder();
            response.append("<html><head><title>Orar FCIM</title>");
            response.append("<style>");
            response.append("body { font-family: Arial, sans-serif; background-color: #f4f4f9; text-align: center; }");
            response.append("h1 { color: #2c3e50; }");
            response.append("form { margin: 20px auto; padding: 15px; background: #fff; display: inline-block; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }");
            response.append("input[type=text], input[type=date] { padding: 8px; border-radius: 4px; border: 1px solid #ccc; margin: 5px; }");
            response.append("input[type=submit], button { padding: 8px 16px; margin-top: 10px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; }");
            response.append("input[type=submit]:hover, button:hover { background: #2980b9; }");
            response.append(".reset-btn { padding: 8px 16px; margin-top: 10px; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer; margin-left: 10px; }");
            response.append(".reset-btn:hover { background: #c0392b; }");
            response.append(".required { color: red; font-weight: bold; margin-left: 2px; }");
            response.append("table { margin: 20px auto; border-collapse: collapse; width: 90%; background: white; box-shadow: 0 0 10px rgba(0,0,0,0.1);} ");
            response.append("th, td { border: 1px solid #ccc; padding: 10px; text-align: center; }");
            response.append("th { background: #3498db; color: white; }");
            response.append("tr:nth-child(even) { background: #f9f9f9; }");
            response.append("tr:hover { background: #f1f1f1; }");
            response.append("</style></head><body>");

            response.append("<h1>Orar FCIM <span style='font-size:0.6em; display:block;'>🍂 Sesiune de toamnă 🍂</span> <span style='font-size:0.6em; display:block;'>📅 (frecvență redusă) 🎓</span></h1>");
            response.append("<form method='GET' action='/'>");
            response.append("Grupa: <span class='required'>*</span> <input type='text' name='grupa' placeholder='ex: CR-221FR'><br>");
            response.append("Data: <input type='date' name='data'><br>");
            response.append("Profesor: <input type='text' name='profesor' placeholder='ex: Rotaru L.'><br>");
            response.append("<input type='submit' value='Caută'> ");
            response.append("<button type='button' class='reset-btn' onclick=\"this.form.reset(); window.location='/'\">Reset Filtre</button>");
            response.append("</form><br>");

            if (grupa != null) {
                List<OrarEntry> orar = ORARE.get(grupa);
                if (orar != null) {
                    List<OrarEntry> filtrat = new ArrayList<>();
                    for (OrarEntry entry : orar) {
                        boolean match = true;
                        if (data != null && !data.isEmpty() && !entry.data.equals(data)) match = false;
                        if (profesor != null && !profesor.isEmpty() &&
                                !entry.profesor.toLowerCase().contains(profesor.toLowerCase())) match = false;
                        if (match) filtrat.add(entry);
                    }

                    if (!filtrat.isEmpty()) {
                        response.append("<h2>Rezultate pentru ").append(grupa).append("</h2>");
                        response.append("<table>");
                        response.append("<tr><th>Ziua</th><th>Data</th><th>Ora</th><th>Tipul</th><th>Disciplina</th><th>Profesorul</th><th>Sala</th></tr>");
                        for (OrarEntry entry : filtrat) {
                            response.append("<tr>");
                            response.append("<td>").append(entry.ziua).append("</td>");
                            response.append("<td>").append(entry.data).append("</td>");
                            response.append("<td>").append(entry.ora).append("</td>");
                            response.append("<td>").append(entry.tip).append("</td>");
                            response.append("<td>").append(entry.disciplina).append("</td>");
                            response.append("<td>").append(entry.profesor).append("</td>");
                            response.append("<td>").append(entry.sala).append("</td>");
                            response.append("</tr>");
                        }
                        response.append("</table>");
                        // Buton Exportă PDF vizibil doar după filtrare
                        String params = "?grupa=" + grupa +
                                (data != null && !data.isEmpty() ? "&data=" + data : "") +
                                (profesor != null && !profesor.isEmpty() ? "&profesor=" + profesor : "");
                        response.append("<form method='GET' action='/export-pdf'>");
                        response.append("<input type='hidden' name='grupa' value='" + grupa + "'>");
                        if (data != null) response.append("<input type='hidden' name='data' value='" + data + "'>");
                        if (profesor != null) response.append("<input type='hidden' name='profesor' value='" + profesor + "'>");
                        response.append("<button type='submit'>📄 Exportă PDF</button>");
                        response.append("</form>");
                    } else {
                        response.append("<p style='color:red;'>Nu există rezultate pentru filtrele aplicate.</p>");
                    }
                } else {
                    response.append("<p style='color:red;'>Nu există orar pentru grupa: ").append(grupa).append("</p>");
                }
            }

            response.append("</body></html>");

            // Setăm explicit UTF-8 în header și scriem răspunsul
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);

            // Cod raspuns HTTP 200 OK
            exchange.sendResponseHeaders(200, response.toString().getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.toString().getBytes());
            }
        }
    }

    // Metoda utilitară pentru parsarea query-ului
    static Map<String, String> parseQuery(String query) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    // Handler export PDF
    static class PdfHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = WebServer.parseQuery(query);

            String grupa = params.get("grupa");
            String data = params.get("data");
            String profesor = params.get("profesor");

            if (grupa == null || !ORARE.containsKey(grupa)) {
                String err = "Parametri PDF lipsă sau grupă invalidă.";
                exchange.sendResponseHeaders(400, err.length());
                exchange.getResponseBody().write(err.getBytes());
                exchange.close();
                return;
            }

            // Filtrare duplicată (la fel ca în handlerul HTML)
            List<OrarEntry> filtrat = new ArrayList<>();
            for (OrarEntry entry : ORARE.get(grupa)) {
                boolean match = true;
                if (data != null && !data.isEmpty() && !entry.data.equals(data)) match = false;
                if (profesor != null && !profesor.isEmpty() &&
                        !entry.profesor.toLowerCase().contains(profesor.toLowerCase())) match = false;
                if (match) filtrat.add(entry);
            }

            if (filtrat.isEmpty()) {
                String msg = "Nu există rezultate pentru exportul PDF.";
                exchange.sendResponseHeaders(404, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
                return;
            }

            // Setăm PDF ca răspuns (indicăm numele fișierului)
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.getResponseHeaders().add("Content-Disposition",
                    "attachment; filename=\"orar_" + grupa + ".pdf\"");

            ByteArrayOutputStream pdfBytes = new ByteArrayOutputStream();

            try {
                Document document = new Document();
                PdfWriter.getInstance(document, pdfBytes);
                document.open();

                // Utilizăm font extern ce suportă Unicde (diacritice românești)
                String fontPath = "src/main/resources/fonts/DejaVuSans.ttf";

                BaseFont bf = BaseFont.createFont(
                        fontPath,
                        BaseFont.IDENTITY_H, // mod encoding Unicode (nu ANSI)
                        BaseFont.EMBEDDED // include în PDF, făcându-l portabil
                );

                Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                Font bodyFont = new Font(bf, 11);

                // Titlu document PDF cu font indicat
                Paragraph title = new Paragraph("Orar – " + grupa, titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Tabel PDF cu 7 coloane
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);

                String[] headers = {"Ziua", "Data", "Ora", "Tip", "Disciplina", "Profesor", "Sala"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(cell);
                }

                for (OrarEntry entry : filtrat) {
                    table.addCell(new PdfPCell(new Paragraph(entry.ziua, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.data, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.ora, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.tip, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.disciplina, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.profesor, bodyFont)));
                    table.addCell(new PdfPCell(new Paragraph(entry.sala, bodyFont)));
                }

                document.add(table);
                document.close();

            } catch (DocumentException e) {
                e.printStackTrace();
            }

            byte[] output = pdfBytes.toByteArray();
            exchange.sendResponseHeaders(200, output.length);
            OutputStream os = exchange.getResponseBody();
            os.write(output);
            os.close();
        }
    }
}
