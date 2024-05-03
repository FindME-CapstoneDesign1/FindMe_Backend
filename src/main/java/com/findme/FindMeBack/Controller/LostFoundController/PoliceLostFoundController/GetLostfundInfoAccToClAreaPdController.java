/*분류별, 지역별, 기간별 습득물 정보 조회 (색상 제외)*/
package com.findme.FindMeBack.Controller.LostFoundController.PoliceLostFoundController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
public class GetLostfundInfoAccToClAreaPdController {

    @PostMapping("/api-police-find-with-date")
    public List<Item> PoliceFindWithDate(@RequestBody SearchItemsWithDate items) throws IOException {
        // 경찰청 API URL 생성
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=4CJYgVugQHbEfSLvdMoGGPFz4Ms%2BrbiUxEk555iigL9ledz0QFEjxOD1mXDCTP0Ziu5%2FHJQ2bYkUTshjquNArg%3D%3D");
        urlBuilder.append("&" + URLEncoder.encode("PRDT_CL_CD_01","UTF-8") + "=" + URLEncoder.encode(items.getPRDT_CL_CD_01() != null ? items.getPRDT_CL_CD_01() : "", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("PRDT_CL_CD_02","UTF-8") + "=" + URLEncoder.encode(items.getPRDT_CL_CD_02() != null ? items.getPRDT_CL_CD_02() : "", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("START_YMD","UTF-8") + "=" + URLEncoder.encode(items.getSTART_YMD() != null ? items.getSTART_YMD() : "", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("END_YMD","UTF-8") + "=" + URLEncoder.encode(items.getEND_YMD() != null ? items.getEND_YMD() : "", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("N_FD_LCT_CD","UTF-8") + "=" + URLEncoder.encode(items.getN_FD_LCT_CD() != null ? items.getN_FD_LCT_CD() : "", "UTF-8"));

        // 페이지 번호와 결과 수 설정
        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("10", "UTF-8")); /*한 페이지 결과 수*/

        // HTTP 연결 설정
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        // 응답 처리
        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        // JSON을 객체로 변환하여 반환
        return jsonToObject(xmlToJson(sb.toString()), items);
    }

    // JSON 문자열을 객체로 변환하는 메서드
    private List<Item> jsonToObject(String jsonInput, SearchItemsWithDate items) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Item> itemList = new ArrayList<>();
        try {
            LostItemsResponse response = objectMapper.readValue(jsonInput, LostItemsResponse.class);
            if (response != null && response.getResponse() != null && response.getResponse().getBody() != null && response.getResponse().getBody().getItems() != null) {
                Object apiItems = response.getResponse().getBody().getItems();
                if (apiItems instanceof List) {
                    itemList = (List<Item>) apiItems;
                } else if (apiItems instanceof Map) {
                    Map<String, Item> itemMap = (Map<String, Item>) apiItems;
                    itemList = new ArrayList<>(itemMap.values());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return itemList;
    }

    // XML을 JSON으로 변환하는 메서드
    private String xmlToJson(String str) {
        try {
            if (str == null) {
                return null; // 문자열이 null이면 null을 반환
            }

            String xml = str;
            JSONObject jObject = XML.toJSONObject(xml);
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            Object json = mapper.readValue(jObject.toString(), Object.class);
            return mapper.writeValueAsString(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 분실물 항목에 대한 정보 저장 클래스
    @Getter
    @Setter
    public static class Item {
        private String fdSbjt; // 제목
        private String rnum;   // 순번
        private String atcId;  // 아이디
        private String fdFilePathImg;  // 이미지 파일 경로
        private String fdSn;   // 일련번호
        private String depPlace;  // 습득 장소
        private String prdtClNm;  // 재품 분류명
        private Date fdYmd;       // 분실 일자
        private String fdPrdtNm;  // 제품 이름
    }

    // 분실물 정보 조회 시 필요한 파라미터들 저장 클래스
    @Getter
    @Setter
    public static class SearchItemsWithDate {
        public String PRDT_CL_CD_01;  // 대분류 코드
        public String PRDT_CL_CD_02;  // 중분류 코드
        public String START_YMD;      // 검색 시작일
        public String END_YMD;        // 검색 종료일
        public String N_FD_LCT_CD;    // 습득지역 코드
    }

    // API 응답 내용 저장 클래스
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;

        @Getter
        @Setter
        public static class Header {
            private String resultCode;
            private String resultMsg;
        }

        @Getter
        @Setter
        public static class Body {
            private int pageNo;
            private int totalCount;
            private int numOfRows; // numOfRows 필드 추가
            private Object items;
        }
    }

    // API 응답 전체 내용 저장 클래스
    @Getter
    @Setter
    public static class LostItemsResponse {
        @JsonProperty("response")
        private Response response;
    }
}
