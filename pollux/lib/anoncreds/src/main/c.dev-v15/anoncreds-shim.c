#include "libanoncreds.h"
#include <stdio.h>
#include <stdint.h>   // for the typedefs (redundant, actually)
#include <inttypes.h>

FfiStrList ffiStrList(const char **handles, size_t count) {
    FfiStrList result = {
         count, handles
    };
    return result;
}

FfiList_ObjectHandle ffiListObjectHandles(const size_t *handles, size_t count) {
        FfiList_ObjectHandle result = {
             count, handles
        };

        return result;
}

ErrorCode shim_anoncreds_encode_credential_attributes(const char **attr_raw_values, size_t count, const char **result_p) {

   FfiStrList sl = {
       count, attr_raw_values
   };
   return anoncreds_encode_credential_attributes(sl, result_p);
}

ErrorCode shim_anoncreds_create_schema(
                                        FfiStr schema_name,
                                        FfiStr schema_version,
                                        FfiStr issuer_id,
                                        const char **attr_names,
                                        size_t attr_names_len,
                                        ObjectHandle *result_p) {
     FfiStrList sl = {
            attr_names_len, attr_names
     };

    return anoncreds_create_schema(schema_name, schema_version, issuer_id, sl, result_p);

}

ErrorCode anoncreds_presentation_request_from_json(ByteBuffer bb, ObjectHandle *result_ptr);

ErrorCode shim_anoncreds_presentation_request_from_json(unsigned char *json_ptr, int64_t json_len, ObjectHandle *result_ptr) {

    ByteBuffer bb = {
       json_len, json_ptr
    };

    ErrorCode result = anoncreds_presentation_request_from_json(bb, result_ptr);
    return result;
}

ErrorCode shim_anoncreds_object_get_json(ObjectHandle handle, unsigned char **buf_ptr) {

    ByteBuffer bb;
    ErrorCode result = anoncreds_object_get_json(handle, &bb);
    *buf_ptr = bb.data;
    return result;
}

ErrorCode shim_anoncreds_create_credential(
                                       ObjectHandle cred_def,
                                       ObjectHandle cred_def_private,
                                       ObjectHandle cred_offer,
                                       ObjectHandle cred_request,
                                       const char **attr_names, size_t attr_names_len,
                                       const char **attr_raw_values, size_t attr_raw_values_len,
                                       const char **attr_enc_values, size_t attr_enc_values_len,
                                       FfiStr rev_reg_id,
                                       ObjectHandle rev_status_list,
                                       ObjectHandle revocation_reg_def,
                                       ObjectHandle revocation_reg_def_private,
                                       int64_t revocation_reg_idx,
                                       FfiStr revocation_tails_path,
                                       ObjectHandle *cred_p
                                       ) {

    FfiStrList attr_names_s = {
        attr_names_len, attr_names
    };

    FfiStrList attr_raw_values_s = {
        attr_raw_values_len, attr_raw_values
    };

    FfiStrList attr_enc_values_s = {
        attr_enc_values_len, attr_enc_values
    };


    FfiCredRevInfo revocation = {
        revocation_reg_def, revocation_reg_def_private, revocation_reg_idx, revocation_tails_path
    };

    return anoncreds_create_credential(
        cred_def,
        cred_def_private,
        cred_offer,
        cred_request,
        attr_names_s,
        attr_raw_values_s,
        attr_enc_values_s,
        rev_reg_id,
        rev_status_list,
        &revocation,
        cred_p
    );

}

ErrorCode shim_anoncreds_create_presentation(ObjectHandle pres_req,
                                         const size_t *credentials_credential,
                                         const int64_t *credentials_timestamp,
                                         const size_t *credentials_rev_state,
                                         const size_t credentials_count,

                                         const int64_t *credentials_prove_entry_idx,
                                         const char **credentials_prove_referent,
                                         const int8_t *credentials_prove_is_predicate,
                                         const int8_t *credentials_prove_reveal,
                                         size_t credentials_prove_count,

                                         const char **self_attest_names,
                                         size_t self_attest_names_count,
                                         const char **self_attest_values,
                                         size_t self_attest_values_count,

                                         ObjectHandle master_secret,

                                         const size_t *schemas,
                                         size_t schemas_count,

                                         const char **schema_ids,
                                         size_t schema_ids_count,

                                         const size_t *cred_defs,
                                         size_t cred_defs_count,

                                         const char **cred_def_ids,
                                         size_t cred_def_ids_count,

                                         ObjectHandle *presentation_p) {


    FfiCredentialEntry entries[credentials_count];
        size_t a;
        for(a = 0; a < credentials_count; a++) {
            entries[a].credential = credentials_credential[a];
            entries[a].timestamp = credentials_timestamp[a];
            entries[a].rev_state = credentials_rev_state[a];
        }


        FfiList_FfiCredentialEntry ffiList_FfiCredentialEntry = {
            credentials_count, entries
        };


  FfiCredentialProve credentials_prove[credentials_prove_count];

    for(a = 0; a < credentials_prove_count; a++) {
        credentials_prove[a].entry_idx = credentials_prove_entry_idx[a];
        credentials_prove[a].referent = credentials_prove_referent[a];
        credentials_prove[a].is_predicate = credentials_prove_is_predicate[a];
        credentials_prove[a].reveal = credentials_prove_reveal[a];
    }

    FfiList_FfiCredentialProve ffiList_FfiCredentialProve = {
        credentials_prove_count, credentials_prove
    };

    FfiStrList self_attest_names_s = {
     self_attest_names_count, self_attest_names
    };

    FfiStrList self_attest_values_s = {
         self_attest_values_count, self_attest_values
    };

    FfiList_ObjectHandle schemas_s = {
             schemas_count, schemas
        };

    FfiStrList schema_ids_s = {
             schema_ids_count, schema_ids
     };

    FfiList_ObjectHandle cred_defs_s = {
             cred_defs_count, cred_defs
     };
    FfiStrList cred_def_ids_s = {
             cred_def_ids_count, cred_def_ids
     };

     return anoncreds_create_presentation(pres_req,
                                                    ffiList_FfiCredentialEntry,
                                                    ffiList_FfiCredentialProve,
                                                    self_attest_names_s,
                                                    self_attest_values_s,
                                                    master_secret,
                                                    schemas_s,
                                                    schema_ids_s,
                                                    cred_defs_s,
                                                    cred_def_ids_s,
                                                    presentation_p);
}

ErrorCode shim_anoncreds_verify_presentation(ObjectHandle presentation,
    ObjectHandle pres_req,
    const size_t *schemas,
    size_t schemas_count,
    const char **schema_ids,
    size_t schema_ids_count,
    const size_t *cred_defs,
    size_t cred_defs_count,
    const char **cred_def_ids,
    size_t cred_def_ids_count,
    const size_t *rev_reg_defs,
    size_t rev_reg_defs_count,
    const char **rev_reg_def_ids,
    size_t rev_reg_def_ids_count,
    const size_t *rev_status_list,
    size_t rev_status_list_count,
    const size_t *nonrevoked_interval_override, //struct FfiNonrevokedIntervalOverride
    size_t nonrevoked_interval_override_count,
    const size_t rev_reg_def_id,
    const size_t requested_from_ts,
    const size_t override_rev_status_list_ts ,
    char *result) {


    FfiList_ObjectHandle schemas_struct = {
                 schemas_count, schemas
            };

    FfiList_ObjectHandle cred_defs_struct = {
                             cred_defs_count, cred_defs
                        };

    FfiList_ObjectHandle rev_reg_defs_struct = {
                 rev_reg_defs_count, rev_reg_defs
            };

    FfiList_ObjectHandle rev_status_list_struct = {
                 rev_status_list_count, rev_status_list
            };

    FfiStrList schema_ids_struct = {
         schema_ids_count, schema_ids
    };

    FfiStrList cred_def_ids_struct = {
             cred_def_ids_count, cred_def_ids
        };

    FfiStrList rev_reg_def_ids_struct = {
             rev_reg_def_ids_count, rev_reg_def_ids
        };
    

    FfiNonrevokedIntervalOverride nonrevoked_interval_override_data_struct = {
        rev_reg_def_id,// FfiStr -> typedef const char *FfiStr;
        requested_from_ts, //int32_t
        override_rev_status_list_ts // int32_t
    };

    FfiList_FfiNonrevokedIntervalOverride nonrevoked_interval_override_struct = {
      nonrevoked_interval_override_count, nonrevoked_interval_override_data_struct 
    };

    int8_t out = 0;
    ErrorCode err = anoncreds_verify_presentation(presentation,
        pres_req,
        schemas_struct,
        schema_ids_struct,
        cred_defs_struct,
        cred_def_ids_struct,
        rev_reg_defs_struct,
        rev_reg_def_ids_struct,
        rev_status_list_struct,
        nonrevoked_interval_override_struct,  // struct FfiList_FfiNonrevokedIntervalOverride 
        &out);


    sprintf(result, "%" PRIi8 "", out);
    return err;
}
