package com.cratos.platf.letter;

import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "邮件信息历史表")
public class LetterRecordHis extends LetterRecord {

    @Id
    @Column(length = 64, comment = "邮件ID(32位); 值=类型(2位)+'-'+user36id(6位)+'-'+fromuser36id(6位)+'-'+随机数(5位)+'-'+create36time(9位)")
    private String letterid = "";

    @Column(comment = "迁移时间，单位毫秒")
    private long movetime;

    @Override
    public void setLetterid(String letterid) {
        this.letterid = letterid;
    }

    @Override
    public String getLetterid() {
        return this.letterid;
    }

    public void setMovetime(long movetime) {
        this.movetime = movetime;
    }

    public long getMovetime() {
        return this.movetime;
    }
}
