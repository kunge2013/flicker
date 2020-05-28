/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.vip;

import com.cratos.platf.info.AwardRecord;
import javax.persistence.Table;
import org.redkale.source.DistributeTable;

/**
 *
 * @author zhangjx
 */
@Table(comment = "摇奖记录表")
@DistributeTable(strategy = AwardRecord.TableStrategy.class)
public class VipAwardRecord extends AwardRecord {

}
