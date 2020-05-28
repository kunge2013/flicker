package com.cratos.platf.duty;

import com.cratos.platf.info.AwardRecord;
import javax.persistence.*;
import org.redkale.source.DistributeTable;

/**
 *
 * @author zhangjx
 */
@Table(comment = "摇奖记录表")
@DistributeTable(strategy = AwardRecord.TableStrategy.class)
public class DutyAwardRecord extends AwardRecord {

}
