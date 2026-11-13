# 개인정보처리방침 영어판 초안 (Notion 붙여 넣기용)

- 작성: 2026-11-13, launch (#144). 1차 글로벌 출시(미국·캐나다·영국·아일랜드·호주·뉴질랜드·인도·필리핀) 준비.
- 원문: 현재 Notion 방침(한국어, 시행일 2026-11-12, 1~9절). 2026-11-13에 끝까지 스크롤해 확인했고 **영어 내용은 없다.**
- 이 초안은 한국어 1~9절을 옮기고, 1차 국가에 필요한 내용(영국 UK GDPR 권리·법적 근거, 미국 주법의 "판매·공유 거부", 국외 이전)을 더했다. **법률 검토를 받은 문서가 아니다.**
- 앱의 방침 링크(`url_privacy_policy`)는 언어와 상관없이 같은 Notion 주소 하나다(`translatable="false"`). 그래서 **같은 페이지의 한국어 아래에 영어를 붙이는 방식**을 권장한다. 페이지를 나누면 앱 코드 변경(언어별 URL)이 필요하다.

## 붙여 넣는 방법

1. Notion 방침 페이지 맨 위에 한 줄을 넣는다: `English version below / 영어 원문은 아래에 있습니다.`
2. 한국어 9절 아래에 구분선(`---`)을 넣고, 아래 "본문" 블록 안의 내용을 그대로 붙여 넣는다(Notion은 마크다운 제목·목록을 자동 변환한다).
3. `[CONTACT EMAIL]`을 한국어 9절과 **같은 문의 이메일**로 바꾼다. 이 저장소에는 이메일을 적지 않는다.
4. 시행일을 게시 날짜로 맞춘다. 한국어 쪽에도 영국·미국 권리 문단(아래 6·7절)을 번역해 넣을지 정한다(넣으면 한국어 시행일도 갱신).
5. 앱 기능이 바뀌면(자막·설정·Wi-Fi 전용 등) 2절 목록을 실제 동작과 다시 맞춘다.

## 본문

```markdown
# PiecePlay: Torrent Player — Privacy Policy

Effective date: [DATE]

## 1. Overview

PiecePlay ("the app") plays a video while it is still downloading from a magnet link or a .torrent link. The developer does not run any server and does not collect your personal information or send it to the developer. However, some information is stored on your device, and some information is shared with third parties through advertising and peer-to-peer (P2P) networking, as described below.

## 2. Information stored only on your device

- Your playlist (links you entered, video titles and resume positions), downloaded video files, download state, and app settings (such as Wi-Fi only and subtitle display).
- The playlist and download state are encrypted with the Android Keystore. This information is never sent to the developer or to cloud backup.
- You can delete it at any time in the app's "Playlist" or "Stored files" screens, and it is all deleted when you uninstall the app.

## 3. P2P (torrent) networking and your IP address

Because of how BitTorrent works, while a video is downloading your IP address is visible to other users sharing the same file (peers), to trackers, and to the DHT network. This is not information the developer collects; it is made public by P2P networking itself. The app is set not to upload (seed) files to other users.

## 4. Advertising

The app shows ads from Google AdMob. To serve and measure ads, Google may collect your advertising ID, device information, approximate location (based on your IP address), and app usage information such as ad views and taps, as well as diagnostics.

- Where the law requires it (for example in the United Kingdom), you are asked for consent through Google's consent tool (User Messaging Platform) before personalised ads are shown.
- Where an ad privacy choice is available to you (for example in the UK and in some US states), you can change it at any time from "Ad privacy settings" in the app.
- You can reset or delete your advertising ID in your device settings.
- Google's processing is described in the Google Privacy Policy (https://policies.google.com/privacy) and in "How Google uses information from sites or apps that use our services" (https://policies.google.com/technologies/partner-sites).

## 5. Permissions

- Internet: torrent connections and ads
- Network state and Wi-Fi state: checking your connection (including the Wi-Fi only setting)
- Advertising ID: used by the Google Mobile Ads SDK

The app does not access your contacts, photos, precise location (GPS), camera or microphone.

## 6. Your rights (United Kingdom)

For users in the United Kingdom, the developer does not hold any of your personal data. Personal data processed for advertising is processed by Google on the basis of your consent where consent is required. You can withdraw or change your consent at any time from "Ad privacy settings" in the app. You also have the right to complain to the Information Commissioner's Office (https://ico.org.uk).

## 7. Your rights (United States)

The developer does not sell your personal information. Google's ad services may be treated as "selling" or "sharing" personal information for targeted advertising under some US state privacy laws. If you live in one of those states, you can opt out from "Ad privacy settings" in the app, and you can also limit ad personalisation in your device settings.

## 8. International transfers

Information collected by Google for advertising may be processed in the United States and other countries where Google operates, under Google's own safeguards.

## 9. Children

The app is not directed at children under 18, and the developer does not knowingly collect information from children.

## 10. Your responsibility for content

The app is a tool that plays links you enter. Play only content you have the right to use.

## 11. Changes to this policy

If this policy changes, the new version will be posted on this page with an updated effective date.

## 12. Contact

For privacy questions or requests: [CONTACT EMAIL]
```

## 한국어 원문과 다른 점

| 절 | 바뀐 점 | 이유 |
|---|---|---|
| 2 | 앱 설정(Wi-Fi 전용, 자막 표시)을 기기 저장 항목에 추가 | 설정 화면이 생김(PATCH_NOTES). 한국어 원문에도 같은 줄을 넣는 것을 권장 |
| 4 | "영국 등에서 동의", "영국·미국 일부 주에서 선택 변경" 문장 | AdMob 유럽 규정 메시지 대상에 영국 포함, 미국 주 메시지는 "광고 개인정보 설정"에서 거부 |
| 4 | Google 광고 안내 링크를 `technologies/partner-sites`로 | AdMob 사용 앱에 Google이 안내하는 페이지. 한국어 원문의 `technologies/ads`도 유효 |
| 6·7·8 | 새로 추가 | UK GDPR(동의 철회, ICO 민원), 미국 주법 판매·공유 거부, 국외 이전 |
| 9 | "만 18 미만" 그대로 | Play 타깃 연령 18세 이상과 맞춤 |

인도(DPDP Act 2023)·필리핀(Data Privacy Act 2012)·캐나다(PIPEDA)·호주(Privacy Act 1988)도 개인정보 처리 고지를 요구하지만, 개발자가 직접 수집하는 정보가 없어 위 공통 문안으로 충분하다고 본다(법률 의견 아님, 미확인).
