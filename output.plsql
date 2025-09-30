CREATE OR REPLACE PACKAGE BODY PCDB.p_courier_fee_for_fiscal_type AS

  FUNCTION row_equals(first_row  PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE,
                     second_row PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE)
    RETURN BOOLEAN IS
  BEGIN
    IF first_row.offer_id = second_row.offer_id
       AND first_row.fiscal_type_id = second_row.fiscal_type_id
       AND first_row.courier_fee_gross = second_row.courier_fee_gross
       AND first_row.courier_fee_net = second_row.courier_fee_net
       AND first_row.create_date = second_row.create_date
       AND first_row.create_user = second_row.create_user
       AND first_row.created_by = second_row.created_by
       AND first_row.change_date = second_row.change_date
       AND first_row.change_user = second_row.change_user
       AND first_row.changed_by = second_row.changed_by THEN
      RETURN TRUE;
    ELSE
      RETURN FALSE;
    END IF;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN FALSE;
  END row_equals;

  FUNCTION add(p_courier_fee_for_fiscal_type PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE,
               p_user VARCHAR2)
    RETURN NUMBER IS
  BEGIN
    INSERT INTO PCDB.COURIER_FEE_FOR_FISCAL_TYPE
      (offer_id,
       fiscal_type_id,
       courier_fee_gross,
       courier_fee_net,
       create_date,
       create_user,
       created_by,
       change_date,
       change_user,
       changed_by,
       create_date,
       create_user,
       created_by,
       change_date,
       change_user,
       changed_by)
    VALUES
      (p_courier_fee_for_fiscal_type.offer_id,
       p_courier_fee_for_fiscal_type.fiscal_type_id,
       p_courier_fee_for_fiscal_type.courier_fee_gross,
       p_courier_fee_for_fiscal_type.courier_fee_net,
       p_courier_fee_for_fiscal_type.create_date,
       p_courier_fee_for_fiscal_type.create_user,
       p_courier_fee_for_fiscal_type.created_by,
       p_courier_fee_for_fiscal_type.change_date,
       p_courier_fee_for_fiscal_type.change_user,
       p_courier_fee_for_fiscal_type.changed_by,
       SYSDATE,
       p_user,
       p_user,
       SYSDATE,
       p_user,
       p_user);
    RETURN SQL%ROWCOUNT;
  END add;

  FUNCTION modify(p_courier_fee_for_fiscal_type PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE,
                  p_user VARCHAR2)
    RETURN NUMBER IS
  BEGIN
    UPDATE PCDB.COURIER_FEE_FOR_FISCAL_TYPE
       SET courier_fee_gross = p_courier_fee_for_fiscal_type.courier_fee_gross,
           courier_fee_net = p_courier_fee_for_fiscal_type.courier_fee_net,
           create_date = p_courier_fee_for_fiscal_type.create_date,
           create_user = p_courier_fee_for_fiscal_type.create_user,
           created_by = p_courier_fee_for_fiscal_type.created_by,
           change_date = p_courier_fee_for_fiscal_type.change_date,
           change_user = p_courier_fee_for_fiscal_type.change_user,
           changed_by = p_courier_fee_for_fiscal_type.changed_by,
           change_date = SYSDATE,
           change_user = p_user,
           changed_by = p_user
     WHERE offer_id = p_courier_fee_for_fiscal_type.offer_id
       AND fiscal_type_id = p_courier_fee_for_fiscal_type.fiscal_type_id;
    RETURN SQL%ROWCOUNT;
  END modify;

  FUNCTION remove(p_offer_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.offer_id%TYPE,
                  p_fiscal_type_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.fiscal_type_id%TYPE,
                  p_user VARCHAR2)
    RETURN NUMBER IS
  BEGIN
    DELETE FROM PCDB.COURIER_FEE_FOR_FISCAL_TYPE
     WHERE offer_id = p_offer_id
       AND fiscal_type_id = p_fiscal_type_id;
    RETURN SQL%ROWCOUNT;
  END remove;

  FUNCTION exists(p_offer_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.offer_id%TYPE,
                  p_fiscal_type_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.fiscal_type_id%TYPE)
    RETURN BOOLEAN IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM PCDB.COURIER_FEE_FOR_FISCAL_TYPE cfft
     WHERE cfft.offer_id = p_offer_id
       AND cfft.fiscal_type_id = p_fiscal_type_id
       AND rownum = 1;
    RETURN v_count > 0;
  END exists;

  FUNCTION get(p_offer_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.offer_id%TYPE,
               p_fiscal_type_id PCDB.COURIER_FEE_FOR_FISCAL_TYPE.fiscal_type_id%TYPE)
    RETURN PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE IS
    v_result PCDB.COURIER_FEE_FOR_FISCAL_TYPE%ROWTYPE;
  BEGIN
    SELECT *
      INTO v_result
      FROM PCDB.COURIER_FEE_FOR_FISCAL_TYPE cfft
     WHERE cfft.offer_id = p_offer_id
       AND cfft.fiscal_type_id = p_fiscal_type_id
       AND rownum = 1;
    RETURN v_result;
  END get;

END p_courier_fee_for_fiscal_type;
