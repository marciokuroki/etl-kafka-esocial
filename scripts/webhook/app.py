#!/usr/bin/env python3
"""
Webhook Receiver para Alertas do Prometheus Alertmanager
Pipeline ETL eSocial
"""

from flask import Flask, request, jsonify
from datetime import datetime
import json
import logging

app = Flask(__name__)

# Configurar logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def format_alert(alert):
    """Formata um alerta para exibição"""
    status = alert.get('status', 'unknown').upper()
    labels = alert.get('labels', {})
    annotations = alert.get('annotations', {})
    
    # Emoji baseado na severidade
    severity = labels.get('severity', 'info')
    emoji_map = {
        'critical': '🚨',
        'warning': '⚠️',
        'info': 'ℹ️'
    }
    emoji = emoji_map.get(severity, '📢')
    
    output = []
    output.append(f"\n{'='*80}")
    output.append(f"{emoji} ALERTA {status} - {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    output.append(f"{'='*80}")
    output.append(f"\n📊 Nome: {labels.get('alertname', 'N/A')}")
    output.append(f"⚠️  Severidade: {severity.upper()}")
    output.append(f"🔧 Serviço: {labels.get('service', 'N/A')}")
    output.append(f"🏷️  Categoria: {labels.get('category', 'N/A')}")
    output.append(f"📝 Descrição: {annotations.get('description', 'N/A')}")
    
    if annotations.get('action'):
        output.append(f"\n💡 Ações Recomendadas:")
        output.append(annotations['action'])
    
    output.append(f"\n🔗 Runbook: {annotations.get('runbook_url', 'N/A')}")
    output.append(f"⏰ Início: {alert.get('startsAt', 'N/A')}")
    
    if alert.get('endsAt'):
        output.append(f"✅ Fim: {alert.get('endsAt')}")
    
    output.append(f"{'='*80}\n")
    
    return '\n'.join(output)

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "service": "webhook-receiver"}), 200

@app.route('/alerts', methods=['POST'])
def receive_alerts():
    """Recebe alertas do Alertmanager"""
    try:
        alert_data = request.json
        
        if not alert_data:
            return jsonify({"error": "No data received"}), 400
        
        alerts = alert_data.get('alerts', [])
        
        if not alerts:
            logger.warning("Recebido payload sem alertas")
            return jsonify({"status": "received", "count": 0}), 200
        
        logger.info(f"Recebidos {len(alerts)} alerta(s)")
        
        for alert in alerts:
            formatted = format_alert(alert)
            print(formatted)
            logger.info(f"Alerta processado: {alert.get('labels', {}).get('alertname', 'unknown')}")
        
        return jsonify({
            "status": "received",
            "count": len(alerts),
            "timestamp": datetime.now().isoformat()
        }), 200
    
    except Exception as e:
        logger.error(f"Erro ao processar alertas: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/alerts/critical', methods=['POST'])
def receive_critical():
    """Endpoint específico para alertas críticos"""
    logger.critical("🚨 ALERTA CRÍTICO RECEBIDO")
    return receive_alerts()

@app.route('/alerts/warning', methods=['POST'])
def receive_warning():
    """Endpoint específico para alertas de warning"""
    logger.warning("⚠️ ALERTA WARNING RECEBIDO")
    return receive_alerts()

if __name__ == '__main__':
    print("="*80)
    print("🚀 Webhook Receiver para Alertas - Pipeline eSocial")
    print("="*80)
    print(f"📡 Escutando na porta 5001")
    print(f"🔗 Endpoints disponíveis:")
    print(f"   - POST /alerts (todos os alertas)")
    print(f"   - POST /alerts/critical (alertas críticos)")
    print(f"   - POST /alerts/warning (alertas de warning)")
    print(f"   - GET /health (health check)")
    print("="*80)
    print()
    
    app.run(host='0.0.0.0', port=5001, debug=False)
