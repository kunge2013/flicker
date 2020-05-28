/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseBean;
import java.util.*;
import org.redkale.convert.ConvertDisabled;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class GameDismissBean extends BaseBean {

    @Comment("同意解散")
    public static final int DISMISS_ANSWER_OK = 1;

    @Comment("不同意解散")
    public static final int DISMISS_ANSWER_NO = 2;

    @Comment("回答人ID")
    protected int userid;

    @Comment("回答; 0: 未回答; 1:同意解散; 2:不同意解散;")
    protected int answer;

    public GameDismissBean() {
    }

    public GameDismissBean(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getAnswer() {
        return answer;
    }

    public void setAnswer(int answer) {
        this.answer = answer;
    }

    public static class GameDismissGroup extends BaseBean {

        @Comment("请求解散房间的玩家ID")
        protected int askUserid;

        @Comment("回答结果玩家列表")
        protected List<GameDismissBean> beans;

        public GameDismissGroup() {
        }

        public GameDismissGroup(int askUserid) {
            this.askUserid = askUserid;
        }

        public void addDismissBean(GameDismissBean bean) {
            if (bean == null) return;
            if (beans == null) beans = new ArrayList<>();
            this.beans.add(bean);
        }

        public GameDismissBean findDismissBean(final int userid) {
            if (beans == null) return null;
            for (GameDismissBean bean : beans) {
                if (bean.getUserid() == userid) return bean;
            }
            return null;
        }

        @ConvertDisabled
        public boolean isCompleted() {
            if (this.beans == null) return true;
            for (GameDismissBean bean : beans) {
                if (bean.getAnswer() == 0) return false;
            }
            return true;
        }

        @ConvertDisabled
        public boolean isAllAnswerOk() {
            if (this.beans == null) return false;
            for (GameDismissBean bean : beans) {
                if (bean.getAnswer() != DISMISS_ANSWER_OK) return false;
            }
            return true;
        }

        public int getAskUserid() {
            return askUserid;
        }

        public void setAskUserid(int askUserid) {
            this.askUserid = askUserid;
        }

        public List<GameDismissBean> getBeans() {
            return beans;
        }

        public void setBeans(List<GameDismissBean> beans) {
            this.beans = beans;
        }

    }
}
