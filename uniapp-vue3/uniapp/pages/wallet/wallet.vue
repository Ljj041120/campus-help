<template>
  <view class="page">
    <view class="balance-card">
      <text class="b-label">可用余额</text>
      <text class="b-amount">{{ formatMoney(wallet.balance) }}</text>
      <view class="b-row">
        <view class="b-item"><text class="b-num">{{ formatMoney(wallet.frozenAmount) }}</text><text class="b-txt">冻结中</text></view>
        <view class="b-item"><text class="b-num">{{ formatMoney(wallet.totalIncome) }}</text><text class="b-txt">累计收入</text></view>
      </view>
    </view>
    <view class="actions">
      <button class="act-btn withdraw" @click="showWithdraw=true">提现</button>
      <button class="act-btn" @click="loadData">刷新</button>
    </view>
    <view class="section-title">交易流水</view>
    <view class="tx-list">
      <view v-for="tx in transactions" :key="tx.id" class="tx-item">
        <view class="tx-left"><text class="tx-desc">{{ tx.description }}</text><text class="tx-time">{{ formatTime(tx.createdAt) }}</text></view>
        <text class="tx-amount" :class="[tx.amount>=0?'in':'out']">{{ formatTxAmount(tx.amount) }}</text>
      </view>
      <view v-if="transactions.length===0" class="empty">暂无交易记录</view>
    </view>

    <!-- 提现弹窗 -->
    <view class="modal" v-if="showWithdraw" @click="showWithdraw=false">
      <view class="modal-card" @click.stop>
        <text class="modal-title">提现到支付宝</text>
        <input v-model="withdrawAmount" type="digit" placeholder="输入金额（≥10元）" class="m-input" />
        <view class="m-btns">
          <button class="m-cancel" @click="showWithdraw=false">取消</button>
          <button class="m-ok" @click="doWithdraw">确认提现</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const api = require('../../api/request.js')
export default {
  data(){return{wallet:{balance:0,frozenAmount:0,totalIncome:0},transactions:[],showWithdraw:false,withdrawAmount:''}},
  onShow(){this.loadData()},
  methods:{
    async loadData(){
      try{const r=await api.get('/wallet/info');if(r&&r.data)this.wallet=r.data;const tx=await api.get('/wallet/records',{limit:20});if(tx&&tx.data)this.transactions=tx.data}catch(e){}
    },
    formatTime(t){return t?t.substring(0,16):''},
    formatMoney(v){return '¥'+(v||0)},
    formatTxAmount(v){return (v>=0?'+':'')+v},
    async doWithdraw(){
      const a=parseFloat(this.withdrawAmount)
      if(!a||a<10)return uni.showToast({title:'最低提现10元',icon:'none'})
      try{await api.post('/wallet/withdraw?amount='+a);uni.showToast({title:'提现申请已提交',icon:'success'});this.showWithdraw=false;this.loadData()}catch(e){uni.showToast({title:'提现失败',icon:'none'})}
    }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f6fa;padding-bottom:30px}
.balance-card{margin:15px;padding:24px;border-radius:16px;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff}
.b-label{font-size:14px;opacity:.7;display:block}
.b-amount{font-size:36px;font-weight:700;margin:8px 0 16px;display:block}
.b-row{display:flex;gap:20px}
.b-item{flex:1}.b-num{font-size:18px;font-weight:600;display:block}.b-txt{font-size:12px;opacity:.7}
.actions{display:flex;gap:10px;padding:0 15px;margin-bottom:15px}
.act-btn{flex:1;height:40px;background:#fff;border-radius:10px;font-size:14px;border:none;color:#333}.act-btn::after{border:none}.act-btn.withdraw{background:#667eea;color:#fff}
.section-title{font-size:15px;font-weight:700;padding:0 15px 10px}
.tx-list{margin:0 15px;background:#fff;border-radius:12px;overflow:hidden}
.tx-item{display:flex;justify-content:space-between;align-items:center;padding:14px 16px;border-bottom:1px solid #f5f5f5}
.tx-desc{font-size:14px;color:#333;display:block}.tx-time{font-size:11px;color:#bbb;margin-top:2px;display:block}
.tx-amount{font-size:16px;font-weight:600}.tx-amount.in{color:#10b981}.tx-amount.out{color:#f44336}
.empty{text-align:center;padding:40px;color:#999;font-size:14px}
.modal{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.5);z-index:999;display:flex;align-items:flex-end;justify-content:center}
.modal-card{background:#fff;border-radius:20px 20px 0 0;width:100%;padding:24px 20px 40px}
.modal-title{font-size:17px;font-weight:700;text-align:center;display:block;margin-bottom:20px}
.m-input{height:46px;background:#f5f6fa;border-radius:10px;padding:0 15px;font-size:16px;margin-bottom:20px}
.m-btns{display:flex;gap:12px}
.m-cancel{flex:1;height:42px;background:#f0f0f0;border-radius:10px;border:none;font-size:15px}.m-cancel::after{border:none}
.m-ok{flex:2;height:42px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:10px;border:none;color:#fff;font-size:15px;font-weight:600}.m-ok::after{border:none}
</style>
