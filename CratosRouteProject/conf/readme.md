#### 业务模块和网关建立连接

-   登录参数如下 password = md5_32('111111')  如  '111111' =====>>>>>> 96e79218965eb72c92a549dd5a330112
	
	{"account":"13044889046", "password":"96e79218965eb72c92a549dd5a330112"}
	

	INSERT INTO `skywar_platf`.`userdetail`(`userid`, `account`, `username`, `type`, `currgame`, `currgamingtime`, `apposid`, `password`, `mobile`, `shenfenname`, `shenfenno`, `face`, `wxunionid`, `qqunionid`, `cxunionid`, `agencyid`, `agencytime`, `viplevel`, `liveness`, `coins`, `banklevel`, `bankcoins`, `bankpwd`, `paycoins`, `giftcoins`, `regcoins`, `diamonds`, `paydiamonds`, `giftdiamonds`, `regdiamonds`, `coupons`, `paycoupons`, `giftcoupons`, `regcoupons`, `paymoney`, `paycount`, `paytime`, `firstpaymoney`, `firstpaytime`, `onlineseconds`, `lastloginaddr`, `lastloginlongitude`, `lastloginlatitude`, `lastloginstreet`, `lastlogintime`, `loginseries`, `appos`, `apptoken`, `status`, `clanid`, `gender`, `intro`, `pwdillcount`, `email`, `updatetime`, `remark`, `regnetmode`, `regtype`, `regtime`, `regapptoken`, `regagent`, `regaddr`) VALUES (8002719, '13044889046', '乐天散人', 4, '', 0, '', '8b8120ea603414b9dc9a2d7db1b11f05f44ce39d', '13044889046', '', '', '', '', '', '', 0, 0, 0, 0, 0, 0, 0, '', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 0.0000000000000000, 0.0000000000000000, '', 0, 0, '', '', 10, 0, 2, '', 15, '', 0, '', '', 10, 1497974400000, '', '', '127.0.0.1');

	
-  相关参数如下
	
	ws://localhost:10510/ws/wsgame?appagent=skywar%2F0.0.6%3B%20web%2F1.0%3B%20chrome%2F81.0.4044.138%3B%20360*640*1%3B&bean=%7B%22account%22:%2213044889046%22,%20%22password%22:%2296e79218965eb72c92a549dd5a330112%22%7D

