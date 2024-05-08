package com.findme.FindMeBack.Controller.GetItemController.Police.Dto.PoliceDateDto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Item {
    private String fdSbjt; // 제목 ex) "지갑(기타색)을 습득하여 보관하고 있습니다."
    private String rnum;   // 순번 ex) 1
    private String atcId;  // 아이디 ex) F2024050300003462
    private String fdFilePathImg;  // 이미지 파일 경로 ex)"https://www.lost112.go.kr/lostnfs/images/uploadImg/20240503/r_20240503085840234.jpg"
    private String fdSn;   // 일련번호 ex) 1
    private String depPlace;  // 습득 장소 ex) "서울강서경찰서"
    private String prdtClNm;  // 재품 분류명 ex) "지갑 > 남성용 지갑"
    private Date fdYmd;       // 분실 일자 ex) "2024-04-11"
    private String fdPrdtNm;  // 제품 이름 ex) "지갑"
}
