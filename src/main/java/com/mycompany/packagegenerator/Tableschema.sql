create table PCDB.COURIER_FEE_FOR_FISCAL_TYPE
(
  offer_id          NUMBER not null,
  fiscal_type_id    NUMBER not null,
  courier_fee_gross NUMBER(10,2),
  courier_fee_net   NUMBER(10,2),
  create_date       DATE,
  create_user       VARCHAR2(20),
  created_by        VARCHAR2(20),
  change_date       DATE,
  change_user       VARCHAR2(20),
  changed_by        VARCHAR2(20)
);
alter table PCDB.COURIER_FEE_FOR_FISCAL_TYPE
  add constraint OFFER_FISCAL_TYPE_OFFER_ID_PK primary key (OFFER_ID, FISCAL_TYPE_ID);
  
alter table PCDB.COURIER_FEE_FOR_FISCAL_TYPE
  add constraint OFFER_FISCAL_TYPE_FISCAL_TYPE_DICT_ID_FK foreign key (FISCAL_TYPE_ID)
  references PCDB.FISCAL_TYPE_DICT (ID);
alter table PCDB.COURIER_FEE_FOR_FISCAL_TYPE
  add constraint OFFER_FISCAL_TYPE_OFFER_ID_FK foreign key (OFFER_ID)
  references PCDB.OFFERS_NEW (ID);