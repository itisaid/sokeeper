DROP TABLE IF EXISTS `node_online_status`;
DROP TABLE IF EXISTS `resource_subscribe`;
DROP TABLE IF EXISTS `resource_changes`;
DROP TABLE IF EXISTS `association_changes`;
DROP TABLE IF EXISTS `presence_config`;
DROP TABLE IF EXISTS `t_sequence`;
DROP TABLE IF EXISTS `resource_types`;
DROP TABLE IF EXISTS `resources`;
DROP TABLE IF EXISTS `association`;
DROP TABLE IF EXISTS `attributes`;
DROP TABLE IF EXISTS `subject`;
DROP TABLE IF EXISTS `keyword`;
DROP TABLE IF EXISTS `subject_keyword`;


DROP TABLE IF EXISTS `acl_objects_members_roles`;

CREATE TABLE `acl_objects_members_roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protected_resource_id` varchar(254) COLLATE utf8_bin NOT NULL,
  `member` varchar(50) COLLATE utf8_bin NOT NULL,
  `role` varchar(50) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_object` (`protected_resource_id`,`member`,`role`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Data for the table `acl_objects_members_roles` */

insert  into `acl_objects_members_roles`(`id`,`protected_resource_id`,`member`,`role`) values (4,'/','Administrator','/Administrator'),(15,'/UI/','UI.Administrator','/User'),(16,'/UI/admin','UI.Admin.Administrator','/User'),(18,'/UI/organization','UI.Administrator','/User'),(17,'/UI/project','UI.Project.Administrator','/User'),(5,'/project.Project/','Project.Administrator','/project.Project/Administrator'),(8,'/project.Project/','Project.Viewer','/project.Project/Viewer'),(6,'/project.Project/1','Project.1.Administrator','/project.Project/Administrator'),(9,'/project.Project/1','Project.1.Viewer','/project.Project/Viewer'),(7,'/project.Project/2','Project.2.Administrator','/project.Project/Administrator'),(10,'/project.Project/2','Project.2.Viewer','/project.Project/Viewer'),(11,'/structure.Organization/','Organization.Administrator','/structure.Organization/Administrator'),(12,'/structure.Organization/1','Organization.1.Administrator','/structure.Organization/Administrator'),(13,'/structure.Organization/2','Organization.2.Administrator','/structure.Organization/Administrator');

/*Table structure for table `acl_resource_ids` */

DROP TABLE IF EXISTS `acl_resource_ids`;

CREATE TABLE `acl_resource_ids` (
  `module` varchar(100) COLLATE utf8_bin NOT NULL,
  `type` varchar(100) COLLATE utf8_bin NOT NULL,
  `id` varchar(100) COLLATE utf8_bin NOT NULL,
  `acl_resource_id` varchar(254) COLLATE utf8_bin NOT NULL,
  `description` varchar(254) COLLATE utf8_bin DEFAULT NULL,
  UNIQUE KEY `id_acl_resource_ids` (`module`,`type`,`id`,`acl_resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Data for the table `acl_resource_ids` */

insert  into `acl_resource_ids`(`module`,`type`,`id`,`acl_resource_id`,`description`) values ('project','Project','1','/project.Project/1',NULL),('project','Project','1','/structure.Organization/1','Project 1 belongs to Organization 1'),('project','Project','2','/project.Project/2',NULL),('project','Project','2','/structure.Organization/1','Project 2 belongs to Organization 1'),('project','Project','3','/project.Project/3',NULL),('project','Project','3','/structure.Organization/2','Project 3 belongs to Organization 2'),('project','Project','4','/project.Project/4',NULL),('project','Project','4','/structure.Organization/2','Project 4 belongs to Organization 2'),('structure','Organization','1','/structure.Organization/1',NULL),('structure','Organization','2','/structure.Organization/2',NULL);

/*Table structure for table `acl_roles_operatives` */

DROP TABLE IF EXISTS `acl_roles_operatives`;

CREATE TABLE `acl_roles_operatives` (
  `role` varchar(50) COLLATE utf8_bin NOT NULL,
  `operative` varchar(50) COLLATE utf8_bin NOT NULL,
  `description` varchar(100) COLLATE utf8_bin DEFAULT NULL,
  UNIQUE KEY `id_role_operative` (`operative`,`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Data for the table `acl_roles_operatives` */

insert  into `acl_roles_operatives`(`role`,`operative`,`description`) values ('/Administrator','/','administrator for all'),('/project.Project/Administrator','/project.Project/','administrator for all granted projects'),('/structure.Organization/Administrator','/project.Project/','organization owner can perform administrator actions for all projects belongs to the orgs'),('/project.Project/Viewer','/project.Project/view','viewer for all granted projects'),('/structure.Organization/Administrator','/structure.Organization/','administrator for all granted orgnaizations');

CREATE TABLE `t_sequence` (
  `name` varchar(50) NOT NULL,
  `current_value` int(20) NOT NULL,
  `increment` int(10) NOT NULL default '1',
  PRIMARY KEY  (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `node_online_status` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `gmt_create`   datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `gmt_expired`  datetime NOT NULL,
  `node_address` varchar(255) NOT NULL,
  `p_node_address` varchar(255) NOT NULL DEFAULT 'S',
  `is_master`       char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `resource_subscribe` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `gmt_create`   datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `resource_id` varchar(255) NOT NULL,
  `client_address` varchar(255) NOT NULL,
  `server_address` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `resource_changes` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `gmt_create`   datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `resource_id` varchar(255) NOT NULL,
  `changes`     varchar(10) NOT NULL,
  `client_address` varchar(255) NOT NULL,
  `server_address` varchar(255) NOT NULL,
  `seq`  int(20) NOT NULL DEFAULT 1,
  `accept_died_evt` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `association_changes` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `gmt_create`   datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `left_type` varchar(255) NOT NULL,
  `left_id` varchar(255) NOT NULL,
  `right_type` varchar(255) NOT NULL,
  `right_id` varchar(255) NOT NULL,
  `changes`     varchar(10) NOT NULL,
  `client_address` varchar(255) NOT NULL,
  `server_address` varchar(255) NOT NULL,
  `seq`  int(20) NOT NULL DEFAULT 1,
  `association_id`  int(20) NOT NULL ,
  `accept_died_evt` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `resource_types` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `gmt_create`   datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `type_name`             varchar(100) NOT NULL,
  `online_res`            char(1) NOT NULL DEFAULT '0',
  `keep_historic`         char(1) NOT NULL DEFAULT '0',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `presence_config` (
  `config_key`  varchar(255) NOT NULL,
  `config_val`  varchar(255) NOT NULL,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `resources` (
  `id` int(20) NOT NULL                     ,
  `gmt_create`    datetime NOT NULL         ,
  `gmt_modified`  datetime NOT NULL         ,
  `resource_type` varchar(255) NOT NULL     ,
  `resource_name` varchar(255) NOT NULL     ,
  `description`   varchar(255)              ,
  `version`  int(20) NOT NULL DEFAULT  1    ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `association` (
  `id` int(20) NOT NULL                     ,
  `gmt_create`    datetime NOT NULL         ,
  `gmt_modified`  datetime NOT NULL         ,
  `left_id`  int(20) NOT NULL               ,
  `right_id` int(20) NOT NULL               ,
  `changed`  char(1) NOT NULL DEFAULT '0'   ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `attributes` (
  `id` int(20) NOT NULL AUTO_INCREMENT              ,
  `gmt_create`    datetime NOT NULL                 ,
  `gmt_modified`  datetime NOT NULL                 ,
  `owner_id`      int(20) NOT NULL                  ,
  `attr_key`  varchar(255) NOT NULL                 ,
  `attr_val`  varchar(255) NOT NULL                 ,
  `attr_type` varchar(255)                          ,
  `description`   varchar(255)                      ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `subject` (
  `id` int(20) NOT NULL AUTO_INCREMENT              ,
  `external_id` int(20)                             ,
  `gmt_create`    datetime NOT NULL                 ,
  `gmt_modified`  datetime NOT NULL                 , 
  `name`  varchar(255) NOT NULL                     ,
  `hash` int(20) NOT NULL                           ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `keyword` (
  `id` int(20) NOT NULL AUTO_INCREMENT              ,
  `gmt_create`    datetime NOT NULL                 ,
  `gmt_modified`  datetime NOT NULL                 , 
  `hash` int(20) NOT NULL                           ,
  `name`  varchar(255) NOT NULL                     ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `subject_keyword` (
  `id` int(20) NOT NULL AUTO_INCREMENT              ,
  `gmt_create`    datetime NOT NULL                 ,
  `gmt_modified`  datetime NOT NULL                 , 
  `subject_id`  int(20) NOT NULL                    ,
  `keyword_id`  int(20) NOT NULL                    ,
  `keyword_occur`  int(20) NOT NULL                 ,  
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

ALTER TABLE node_online_status  ADD UNIQUE INDEX idx_nos_pid_address(p_node_address,node_address);
ALTER TABLE resource_subscribe  ADD UNIQUE INDEX idx_rsl_resource_node_addr(resource_type,resource_id,client_address,server_address);
ALTER TABLE resource_changes    ADD UNIQUE INDEX idx_rc_resource_type_id(resource_type,resource_id);
ALTER TABLE association_changes ADD UNIQUE INDEX idx_ac_resource_type_id(left_type,left_id,right_type,right_id);
ALTER TABLE association_changes ADD UNIQUE INDEX idx_ac_association_id(association_id);
ALTER TABLE resource_types      ADD UNIQUE INDEX idx_rt_resource_type_name(type_name);
ALTER TABLE resources           ADD UNIQUE INDEX idx_r_resource_type_name(resource_type,resource_name);
ALTER TABLE association         ADD UNIQUE INDEX idx_ass_left_right_ids(left_id,right_id);
ALTER TABLE attributes          ADD UNIQUE INDEX idx_att_owner_id_key(owner_id,attr_key);
ALTER TABLE subject             ADD UNIQUE INDEX subject_name_unique(name);
ALTER TABLE subject             ADD INDEX subject_hash(hash ASC);
ALTER TABLE keyword             ADD UNIQUE INDEX keyword_name_unique(name);
ALTER TABLE keyword             ADD INDEX keyword_hash(hash ASC);
ALTER TABLE subject_keyword     ADD UNIQUE INDEX subject_keyword_key(subject_id,keyword_id);

insert into t_sequence values('resource_changes',1,1);
insert into t_sequence values('resources',1,1);

insert into presence_config values('maxCachedEntities','1000000');
insert into presence_config values('secondsOfResourceChangesWatcherTimer','1');
insert into presence_config values('secondsOfPresenceTimer','5');
insert into presence_config values('secondsOfNodeKeepAlive','20');
insert into presence_config values('suffixOfServer','/server?timeout_ms=5000&max_connections=500&hb_sec=10');

DELIMITER $$
DROP FUNCTION IF EXISTS `currval`$$
CREATE FUNCTION `currval`(seq_name VARCHAR(50)) RETURNS int(20)
BEGIN
	DECLARE value INTEGER;
	SET value = 0;
	SELECT current_value INTO value
	FROM t_sequence
	WHERE name = seq_name;
	RETURN value;
END$$
DELIMITER ;

DELIMITER $$

DROP FUNCTION IF EXISTS `nextval`$$

CREATE FUNCTION `nextval`(seq_name VARCHAR(50)) RETURNS int(20)
BEGIN
	UPDATE t_sequence
	SET	current_value = current_value + increment
	WHERE name = seq_name;
	RETURN currval(seq_name);
END$$

DELIMITER ;
