package jp.co.sss.lms.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.form.StudentsListForm;

/**
 * 勤怠情報（受講生入力）テーブルマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface TStudentAttendanceMapper {

	/**
	 * 勤怠情報（受講生入力）取得（LMSユーザーID）
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return 勤怠情報（受講生入力）エンティティ
	 */
	List<TStudentAttendance> findByLmsUserId(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠情報（受講生入力）取得（LMSユーザーID＆日付）
	 * 
	 * @param lmsUserId
	 * @param trainingDate
	 * @param deleteFlg
	 * @return 勤怠情報（受講生入力）エンティティ
	 */
	TStudentAttendance findByLmsUserIdAndTrainingDate(@Param("lmsUserId") Integer lmsUserId,
			@Param("trainingDate") Date trainingDate, @Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠管理画面用DTOリスト取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return 勤怠管理画面用DTOリスト
	 */
	List<AttendanceManagementDto> getAttendanceManagement(@Param("courseId") Integer courseId,
			@Param("lmsUserId") Integer lmsUserId, @Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠情報（受講生入力）登録
	 * 
	 * @param tStudentAttendance
	 * @return 登録結果
	 */
	Boolean insert(TStudentAttendance tStudentAttendance);

	/**
	 * 勤怠情報（受講生入力）更新
	 * 
	 * @param tStudentAttendance
	 * @return 更新結果
	 */
	Boolean update(TStudentAttendance tStudentAttendance);
	
	
	/**
	 * 勤怠情報（受講生入力）未入力件数取得
	 * @author 吉田令生  – Task.25
	 * @param lmsUserId
	 * @param deleteFlg
	 * @param trainingDate
	 * @return 勤怠未入力数
	 */
	Integer notEnterCount(
			@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg,
			@Param("trainingDate") Date trainingDate);
	
	
	/**
	 * 生徒勤怠情報:取得
	 * @author 吉田令生  – Task.57
	 * @param role
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	List<UserDetailDto> findTodayAttendance(@Param("role") String role,
			@Param("deleteFlg") Short deleteFlg);
	
	/**
	 * 生徒勤怠情報:検索
	 * @author 吉田令生  – Task.57
	 * @param role
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	List<UserDetailDto> searchStudentAttendance(
			@Param("role") String role,
			@Param("deleteFlg") Short deleteFlg,
			@Param("studentsListForm") StudentsListForm studentsListForm
			);

}
