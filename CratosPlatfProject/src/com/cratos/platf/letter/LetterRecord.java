package com.cratos.platf.letter;

import com.cratos.platf.base.*;
import com.cratos.platf.order.GoodsItem;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "未读邮件信息表")
public class LetterRecord extends BaseEntity {

    private static final Reproduce<LetterRecordHis, LetterRecord> reproduce = Reproduce.create(LetterRecordHis.class, LetterRecord.class);

    @Comment("类型: 金钻赠送或奖励")
    public static final short LETTER_TYPE_GIFT = 10;

    @Comment("类型: 信息通知")
    public static final short LETTER_TYPE_NOTICE = 20;

    @Comment("类型: 周期性日奖励")
    public static final short LETTER_TYPE_PERIOD = 30;

    @Comment("状态: 未读")
    public static final short LETTER_STATUS_UNREAD = 10;

    @Comment("状态: 已读")
    public static final short LETTER_STATUS_READED = 20;

    @Comment("状态: 过期")
    public static final short LETTER_STATUS_EXPIRE = 30;

    @Id
    @Column(length = 64, comment = "邮件ID(32位); 值=类型(2位)+'-'+user36id(6位)+'-'+fromuser36id(6位)+'-'+随机数(5位)+'-'+create36time(9位)")
    private String letterid = "";

    @Column(length = 64, comment = "邮件文稿ID， 为空表示非人工发送")
    private String letterdraftid = "";

    @Column(comment = "邮件接收方用户ID")
    private int userid;

    @Column(comment = "邮件发送方用户ID, 为0表示系统发送")
    private int fromuserid;

    @Column(comment = "邮件类型: 10:金钻赠送或奖励;20:信息通知;")
    private short lettertype = 10;

    @Column(comment = "赠送金币数")
    private int coins;

    @Column(comment = "赠送钻石数")
    private int diamonds;

    @Column(comment = "赠送奖券数")
    private int coupons;

    //@Deprecated
    //@Column(length = 255, comment = "其他赠品数据,JSON格式")
    //private String goodsjson = "";
    @Column(length = 4096, nullable = false, comment = "签到领取的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 32, comment = "子模块")
    private String module = "";

    @Column(length = 1024, comment = "记录描述")
    private String remark = "";

    @Column(comment = "状态; 10:未读; 20:已读; 30:过期;")
    private short status = LETTER_STATUS_UNREAD;

    @Column(length = 64, comment = "邮件标题")
    private String title = "";

    @Column(length = 255, comment = "邮件内容")
    private String content = "";

    @Column(comment = "预设时间，单位毫秒, 为0表示及时生效")
    private long starttime;

    @Column(updatable = false, comment = "创建时间，单位毫秒")
    private long createtime;

    @Transient
    @Comment("邮件接收方名称")
    private String tousername = "";

    @Transient
    @Comment("邮件发送方名称")
    private String fromusername = "";

    @Transient
    @Comment("邮件发送方头像")
    private String fromuserface = "";

    @Transient
    @Comment("邮件发送方性别")
    private short fromusergender;

    public LetterRecordHis createLetterRecordHis(short status, long time) {
        LetterRecordHis his = reproduce.apply(new LetterRecordHis(), this);
        his.setMovetime(time);
        his.setStatus(status);
        return his;
    }

    public void setFromuser(UserInfo fromuser) {
        this.fromusername = fromuser == null ? "" : fromuser.getUsername();
        this.fromusergender = fromuser == null ? 0 : fromuser.getGender();
        this.fromuserface = fromuser == null ? "" : fromuser.getFace();
    }

    public void setLetterid(String letterid) {
        this.letterid = letterid;
    }

    public String getLetterid() {
        return this.letterid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getUser36id() {
        return Integer.toString(userid, 36);
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getFromuser36id() {
        if (fromuserid < 1) return "000000";
        return Integer.toString(fromuserid, 36);
    }

    public void setFromuserid(int fromuserid) {
        this.fromuserid = fromuserid;
    }

    public int getFromuserid() {
        return this.fromuserid;
    }

    public void setLettertype(short lettertype) {
        this.lettertype = lettertype;
    }

    public short getLettertype() {
        return this.lettertype;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getCoins() {
        return this.coins;
    }

    public void setDiamonds(int diamonds) {
        this.diamonds = diamonds;
    }

    public int getDiamonds() {
        return this.diamonds;
    }

    public int getCoupons() {
        return coupons;
    }

    public void setCoupons(int coupons) {
        this.coupons = coupons;
    }

    public GoodsItem[] getGoodsitems() {
        return goodsitems;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getStatus() {
        return this.status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getStarttime() {
        return this.starttime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public String getFromusername() {
        return fromusername;
    }

    public void setFromusername(String fromusername) {
        this.fromusername = fromusername;
    }

    public short getFromusergender() {
        return fromusergender;
    }

    public void setFromusergender(short fromusergender) {
        this.fromusergender = fromusergender;
    }

    public String getFromuserface() {
        return fromuserface;
    }

    public void setFromuserface(String fromuserface) {
        this.fromuserface = fromuserface;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }

    public String getTousername() {
        return tousername;
    }

    public void setTousername(String tousername) {
        this.tousername = tousername;
    }

    public String getLetterdraftid() {
        return letterdraftid;
    }

    public void setLetterdraftid(String letterdraftid) {
        this.letterdraftid = letterdraftid;
    }

}
