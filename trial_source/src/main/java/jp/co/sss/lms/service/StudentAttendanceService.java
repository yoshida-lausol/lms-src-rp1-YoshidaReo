package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
			
			
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		attendanceForm.setHourMap(attendanceUtil.getHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.getMinuteMap());
		;

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());
			
			
			dailyAttendanceForm.setDispStartHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingStartTime()));
			dailyAttendanceForm.setDispStartMinute(attendanceUtil.getMinute(attendanceManagementDto.getTrainingStartTime()));
			
			dailyAttendanceForm.setDispEndHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingEndTime()));
			dailyAttendanceForm.setDispEndMinute(attendanceUtil.getMinute(attendanceManagementDto.getTrainingEndTime()));;

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
            TrainingTime trainingStartTime = null;
            trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
            tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
            // 退勤時刻整形
            TrainingTime trainingEndTime = null;
            trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
            tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	
	/**
	 * 現在日付より前日付で勤怠未入力数を取得
	 * @throws ParseException
	 * @author 吉田令生  – Task.25
	 */
	public Boolean notEnterCheck() throws ParseException{
		
		// 本日の研修日の取得
		// Date trainingDate = attendanceUtil.getTrainingDate();
		
		// フォーマットを定義
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d");
        // 現在日時をフォーマットした文字列に
        String dateStr = sdf.format(new Date());
        // 文字列をDate型にパース
        Date trainingDate = sdf.parse(dateStr);
		// 勤怠未入力数の取得
		Integer blankEntryCount = tStudentAttendanceMapper.notEnterCount(
						loginUserDto.getLmsUserId(),
						Constants.DB_FLG_FALSE,
						trainingDate);
//		if(blankEntryCount > 0) {
//			return true;
//		}
//		return false;
		return blankEntryCount > 0;
	}
	
	
	/*
	 * フォーム内の「時」と「分」の入力を、「hh:mm」形式の文字列に変換してAttendanceFormにセットする。
	 * @author 吉田令生  – Task.26
	 * */
	public void formatConversion(AttendanceForm attendanceForm) {
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			if(dailyAttendanceForm.getDispStartHour() != null && dailyAttendanceForm.getDispStartMinute() != null) {
				int sh = dailyAttendanceForm.getDispStartHour();
				int sm = dailyAttendanceForm.getDispStartMinute();
				String startTimeString  = String.format("%02d:%02d", sh, sm);
				dailyAttendanceForm.setTrainingStartTime(startTimeString);
			}
			
			if(dailyAttendanceForm.getDispEndHour() != null && dailyAttendanceForm.getDispEndMinute() != null) {
				int eh = dailyAttendanceForm.getDispEndHour();
				int em = dailyAttendanceForm.getDispEndMinute();
				String endTimeString  = String.format("%02d:%02d", eh, em);
				dailyAttendanceForm.setTrainingEndTime(endTimeString);
			}
			
		}
	}
	
	/*
	 * 勤怠更新時の入力チェック
	 * (文字数、時刻の整合性、中抜け時間の妥当性)
	 * @author 吉田令生  – Task.27
	 * */
	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result) {
		
		int index = 0;
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			// 備考の文字数制限
			if(dailyAttendanceForm.getNote() != null && dailyAttendanceForm.getNote().length() > 100) {
				result.rejectValue(
						"attendanceList["+index+"].note",
						"maxlength",
						new Object[] {"備考", 100},
						"備考の文字数が100文字を超えています。"
						);
			}
			
			// 勤怠リスト[n]．出勤時間（時）、出勤時間（分）の一方が入力有り　＆　もう一方が入力なしの場合
			if(dailyAttendanceForm.getDispStartMinute() != null && dailyAttendanceForm.getDispStartHour() == null) {
				result.rejectValue(
					"attendanceList["+index+"].dispStartHour",
					"input.invalid",
					new Object[] {"出勤時間"},
					"出勤時間が正しく入力されていません。"
				);
				System.out.println("attendanceList["+index+"].dispStartHour");
			}
			if(dailyAttendanceForm.getDispStartHour() != null && dailyAttendanceForm.getDispStartMinute() == null) {
				result.rejectValue(
					"attendanceList["+index+"].dispStartMinute",
					"input.invalid",
					new Object[] {"出勤時間"},
					"出勤時間が正しく入力されていません。"
				);
			}
			
			// 勤怠リスト[n]．退勤時間（時）、退勤時間（分）の一方が入力有り　＆　もう一方が入力なしの場合
			if(dailyAttendanceForm.getDispEndMinute() != null &&  dailyAttendanceForm.getDispEndHour() == null ) {
				result.rejectValue(
						"attendanceList["+index+"].dispEndHour",
						"input.invalid",
						new Object[] {"退勤時間"},
						"出勤時間が正しく入力されていません。"
				);					
			}
			if(dailyAttendanceForm.getDispEndHour() != null && dailyAttendanceForm.getDispEndMinute() == null ) {
				result.rejectValue(
						"attendanceList["+index+"].dispEndMinute",
						"input.invalid",
						new Object[] {"退勤時間"},
						"出勤時間が正しく入力されていません。"
				);					
			}
			
			// 出勤時間に入力なし　＆　退勤時間に入力あり　の場合
			if(dailyAttendanceForm.getDispEndHour() != null && dailyAttendanceForm.getDispEndMinute() != null) {
				if(dailyAttendanceForm.getDispStartHour() == null && dailyAttendanceForm.getDispStartMinute() == null) {
					result.rejectValue(
							"attendanceList["+index+"].dispStartHour",
							null,
							null,
							""
						);
					result.rejectValue(
							"attendanceList["+index+"].dispStartMinute",
							null,
							null,
							""
						);
					result.reject("attendance.punchInEmpty",null,null);
				}
			}
			

			if(dailyAttendanceForm.getDispStartHour() != null && dailyAttendanceForm.getDispStartMinute() != null &&dailyAttendanceForm.getDispEndHour() != null && dailyAttendanceForm.getDispEndMinute() != null) {
				LocalTime localStartTime = LocalTime.of(dailyAttendanceForm.getDispStartHour(), dailyAttendanceForm.getDispStartMinute());
				LocalTime localEndTime = LocalTime.of(dailyAttendanceForm.getDispEndHour(), dailyAttendanceForm.getDispEndMinute());
			//	出勤時間　＞　退勤時間　の場合
				if(localEndTime.isBefore(localStartTime)) {
					result.rejectValue(
							"attendanceList["+index+"].dispEndHour",
							null,
							null,
							""
						);
					result.rejectValue(
							"attendanceList["+index+"].dispEndMinute",
							null,
							null,
							""
						);
					result.reject("attendance.trainingTimeRange",new Object[] {index},"退勤時刻は出勤時刻より後でなければいけません。");
				}
			//	中抜け時間が勤務時間（出勤時間～退勤時間までの時間）を超える場合
				if(dailyAttendanceForm.getBlankTime() != null) {
					Duration localDurationTime = Duration.between(localStartTime, localEndTime);
					long totalMinute = localDurationTime.toMinutes();
					if(totalMinute < dailyAttendanceForm.getBlankTime()) {
						result.rejectValue(
								"attendanceList["+index+"].blankTime",
								"attendance.blankTimeError",
								null,
								""
							);
						
					}
				}
				
			}
			index++;
			
			// select系を再設定
			attendanceForm.setHourMap(attendanceUtil.getHourMap());
			attendanceForm.setMinuteMap(attendanceUtil.getMinuteMap());
			attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		}
	}
}
