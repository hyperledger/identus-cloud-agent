{{- define "cors" }}
    {{- if .Values.ingress.cors.enabled }}
    - name: cors
      enable: true
      {{- if .Values.ingress.cors.allow_origins }}
      config:
        allow_origins: {{ .Values.ingress.cors.allow_origins | quote }}
      {{- end }}
    {{- end }}
{{- end -}}
{{- define "consumer-restriction" }}
    - name: consumer-restriction
      enable: true
      config:
        whitelist:
        {{- range .Values.ingress.consumers }}
          -  {{ regexReplaceAll "-" $.Release.Name "_" }}_{{ regexReplaceAll "-" . "_" | lower }}
        {{- end }}
{{- end -}}
{{- define "labels.common" -}}
app.kubernetes.io/part-of: prism-agent
{{- end -}}
