<template>
  <view class="publish-page">
    <!-- 顶部进度条 -->
    <view class="top-banner">
      <text class="top-title">📝 发布跑腿任务</text>
      <text class="top-sub">填写信息，找人帮你跑腿</text>
    </view>

    <!-- 1. 跑腿类型 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">📌</text><text class="section-title">选择跑腿类型</text></view>
      <view class="type-grid">
        <view v-for="t in types" :key="t.value" class="type-card" :class="{ active: form.orderType === t.value }" @click="form.orderType = t.value">
          <view class="type-icon-wrap"><text class="type-emoji">{{ t.emoji }}</text></view>
          <text class="type-name">{{ t.label }}</text>
          <text class="type-desc">{{ t.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 2. 地址 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">📍</text><text class="section-title">地址信息</text></view>
      <view class="form-item">
        <view class="input-label"><text class="label-dot pickup-dot">取</text><text class="label-text">取货地址</text></view>
        <input v-model="form.pickupAddress" placeholder-class="ph" placeholder="例如：菜鸟驿站、食堂三楼" class="input" />
      </view>
      <view class="form-item" style="margin-top:0">
        <view class="input-label"><text class="label-dot delivery-dot">送</text><text class="label-text">送货地址</text></view>
        <input v-model="form.deliveryAddress" placeholder-class="ph" placeholder="例如：5号宿舍楼、教学楼A座" class="input" />
      </view>
      <view class="route-line2">
        <view class="route-dot2 green"></view><view class="route-bar"></view><view class="route-dot2 red"></view>
      </view>
    </view>

    <!-- 3. 报酬 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">💰</text><text class="section-title">报酬金额</text></view>
      <view class="amount-row">
        <text class="amount-label">¥</text>
        <input v-model="form.amount" type="digit" placeholder-class="ph" placeholder="输入金额（2~50元）" class="amount-input" @input="calcFee" />
      </view>
      <view class="fee-card" v-if="platformFee>0">
        <view class="fee-item"><text class="fee-l">平台服务费(5%)</text><text class="fee-v">-¥{{platformFee}}</text></view>
        <view class="fee-divider"></view>
        <view class="fee-item"><text class="fee-l">跑腿员到手</text><text class="fee-v green">¥{{runnerIncome}}</text></view>
      </view>
    </view>

    <!-- 4. 时间 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">⏰</text><text class="section-title">期望完成时间</text></view>
      <view class="time-row">
        <picker mode="date" :value="form.expectedDate" @change="onDateChange">
          <view class="picker-box"><text class="picker-icon">📅</text><text>{{form.expectedDate||'选择日期'}}</text></view>
        </picker>
        <picker mode="time" :value="form.expectedTime" @change="onTimeChange">
          <view class="picker-box"><text class="picker-icon">🕐</text><text>{{form.expectedTime||'选择时间'}}</text></view>
        </picker>
      </view>
    </view>

    <!-- 5. 描述+备注 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">📋</text><text class="section-title">补充信息（选填）</text></view>
      <textarea v-model="form.description" placeholder-class="ph" placeholder="物品重量、大小、特殊要求..." class="textarea" maxlength="200" />
      <view class="char-count"><text>{{form.description.length}}/200</text></view>
      <input v-model="form.remark" placeholder-class="ph" placeholder="其他要说明的..." class="input" style="margin-top:10px" maxlength="100" />
    </view>

    <!-- 6. 图片 -->
    <view class="section">
      <view class="section-header"><text class="section-icon">🖼️</text><text class="section-title">上传图片</text><text class="section-sub">（最多3张）</text></view>
      <view class="image-list">
        <view v-for="(img,i) in form.images" :key="i" class="image-item" @click="previewImage(i)">
          <image :src="img" mode="aspectFill" class="upload-img" />
          <view class="del-btn" @click.stop="removeImage(i)"><text class="del-x">✕</text></view>
          <view class="img-index"><text>{{i+1}}</text></view>
        </view>
        <view v-if="form.images.length<3" class="add-btn" @click="chooseImage">
          <text class="add-icon">+</text>
          <text class="add-txt">上传</text>
        </view>
      </view>
    </view>

    <!-- 提交 -->
    <view class="submit-area">
      <button class="submit-btn" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
        <text class="btn-text" v-if="!submitting">{{'发布订单 · 支付 ¥'+(form.amount||0)}}</text>
      </button>
      <view class="submit-info">
        <text class="info-icon">ℹ️</text>
        <text class="info-text">发布后进入待支付状态，系统将自动模拟支付</text>
      </view>
    </view>
  </view>
</template>

<script>
const api = require('../../api/request.js')
export default {
  data() {
    const t = new Date()
    const ds = t.getFullYear()+'-'+(t.getMonth()+1).toString().padStart(2,'0')+'-'+t.getDate().toString().padStart(2,'0')
    return {
      types:[
        {v:1,l:'代取快递',e:'📦',desc:'帮取快递送到宿舍'},
        {v:2,l:'代买物品',e:'🛒',desc:'帮忙买东西送过来'},
        {v:3,l:'代送物品',e:'🚀',desc:'帮忙送东西过去'},
        {v:4,l:'其他',e:'📌',desc:'其他跑腿需求'}
      ],
      form:{orderType:null,pickupAddress:'',deliveryAddress:'',amount:'',expectedDate:ds,expectedTime:'',description:'',remark:'',images:[]},
      platformFee:0,runnerIncome:0,submitting:false
    }
  },
  computed:{canSubmit(){return this.form.orderType&&this.form.pickupAddress.trim()&&this.form.deliveryAddress.trim()&&this.form.amount>=2&&this.form.amount<=50&&this.form.expectedDate&&this.form.expectedTime}},
  methods:{
    calcFee(){const a=parseFloat(this.form.amount)||0;this.platformFee=Math.round(a*0.05*100)/100;this.runnerIncome=Math.round((a-this.platformFee)*100)/100},
    onDateChange(e){this.form.expectedDate=e.detail.value},
    onTimeChange(e){this.form.expectedTime=e.detail.value},
    chooseImage(){uni.chooseImage({count:3-this.form.images.length,sizeType:['compressed'],sourceType:['album','camera'],success:async res=>{uni.showLoading({title:'上传中...'});for(const p of res.tempFilePaths){try{const u=await uni.uploadFile({url:api.getBaseUrl()+'/file/upload',filePath:p,name:'file',header:{'Authorization':'Bearer '+uni.getStorageSync('token')}});const d=JSON.parse(u.data);if(d.code===200&&d.data)this.form.images.push(d.data.url)}catch(e){}};uni.hideLoading()}})},
    removeImage(i){this.form.images.splice(i,1)},
    previewImage(i){uni.previewImage({current:i,urls:this.form.images})},
    async handleSubmit(){if(!this.canSubmit)return uni.showToast({title:'请完善信息',icon:'none'});this.submitting=true;try{const r=await api.post('/order/create',{orderType:this.form.orderType,pickupAddress:this.form.pickupAddress.trim(),pickupLng:121.5,pickupLat:31.2,deliveryAddress:this.form.deliveryAddress.trim(),deliveryLng:121.6,deliveryLat:31.3,amount:parseFloat(this.form.amount),expectedTime:this.form.expectedDate+' '+this.form.expectedTime+':00',description:this.form.description,remark:this.form.remark,images:this.form.images.join(',')});uni.showToast({title:'发布成功！',icon:'success'});setTimeout(async()=>{try{await api.post('/order/'+r.data.id+'/pay',{alipayTradeNo:'mock_'+Date.now()})}catch(e){}},1500);setTimeout(()=>uni.switchTab({url:'/pages/index/index'}),2000)}catch(e){uni.showToast({title:'发布失败',icon:'none'})};this.submitting=false}
  }
}
</script>

<style scoped>
.publish-page{min-height:100vh;background:#f5f6fa;padding-bottom:40px}
/* 顶部 */
.top-banner{background:linear-gradient(135deg,#667eea,#764ba2);padding:30px 20px 25px;color:#fff}
.top-title{font-size:22px;font-weight:700;display:block}
.top-sub{font-size:13px;opacity:.8;margin-top:6px;display:block}
/* 分区 */
.section{margin:0 15px 12px;background:#fff;border-radius:16px;padding:18px;box-shadow:0 2px 8px rgba(0,0,0,.04);position:relative;overflow:hidden}
.section-header{display:flex;align-items:center;margin-bottom:14px}
.section-icon{font-size:18px;margin-right:8px}
.section-title{font-size:15px;font-weight:700;color:#333}
.section-sub{font-size:11px;color:#bbb;margin-left:4px}
/* 类型 */
.type-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px}
.type-card{padding:16px 12px;border-radius:14px;text-align:center;border:2px solid #f0f0f0;transition:all .2s}
.type-card.active{border-color:#667eea;background:linear-gradient(135deg,rgba(102,126,234,.06),rgba(118,75,162,.06));transform:translateY(-2px);box-shadow:0 4px 12px rgba(102,126,234,.15)}
.type-icon-wrap{width:48px;height:48px;border-radius:14px;display:flex;align-items:center;justify-content:center;margin:0 auto 8px}
.type-card.active .type-icon-wrap{background:linear-gradient(135deg,#667eea,#764ba2)}
.type-card:not(.active) .type-icon-wrap{background:#f5f6fa}
.type-emoji{font-size:24px}
.type-name{font-size:14px;font-weight:700;color:#333;display:block}
.type-desc{font-size:11px;color:#999;margin-top:3px;display:block}
/* 输入 */
.form-item{margin-bottom:14px;position:relative}
.input-label{display:flex;align-items:center;margin-bottom:6px}
.label-dot{width:20px;height:20px;border-radius:6px;font-size:10px;color:#fff;display:flex;align-items:center;justify-content:center;margin-right:6px;font-weight:700}
.pickup-dot{background:#4caf50}.delivery-dot{background:#f44336}
.label-text{font-size:13px;color:#666}
.input{height:44px;background:#f7f8fc;border-radius:12px;padding:0 16px;font-size:14px;border:1px solid #eee;transition:border .2s}
.input:focus{border-color:#667eea}
.ph{color:#ccc;font-size:13px}
.route-line2{display:flex;align-items:center;justify-content:center;margin:4px 0 6px;gap:4px}
.route-dot2{width:10px;height:10px;border-radius:50%}.route-dot2.green{background:#4caf50}.route-dot2.red{background:#f44336}
.route-bar{width:40%;height:2px;background:#ddd;border-radius:1px}
/* 金额 */
.amount-row{display:flex;align-items:center;background:#fff;border:2px solid #667eea;border-radius:14px;padding:0 16px;height:52px}
.amount-label{font-size:22px;font-weight:700;color:#f44336;margin-right:8px}
.amount-input{flex:1;height:100%;font-size:20px;font-weight:700}
.fee-card{background:#f7f8fc;border-radius:12px;padding:12px 16px;margin-top:10px}
.fee-item{display:flex;justify-content:space-between;padding:4px 0}
.fee-l{font-size:13px;color:#888}
.fee-v{font-size:14px;font-weight:600;color:#333}
.fee-v.green{color:#10b981;font-size:16px}
.fee-divider{height:1px;background:#eee;margin:4px 0}
/* 时间 */
.time-row{display:flex;gap:10px}
.picker-box{flex:1;display:flex;align-items:center;height:44px;background:#f7f8fc;border-radius:12px;padding:0 14px;font-size:14px;color:#333;border:1px solid #eee}
.picker-icon{margin-right:8px;font-size:16px}
/* 文本域 */
.textarea{width:100%;min-height:80px;background:#f7f8fc;border-radius:12px;padding:12px 16px;font-size:14px;box-sizing:border-box;border:1px solid #eee}
.char-count{text-align:right;font-size:11px;color:#bbb;margin-top:4px}
/* 图片 */
.image-list{display:flex;flex-wrap:wrap;gap:10px}
.image-item{position:relative;width:80px;height:80px;border-radius:12px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,.1)}
.upload-img{width:100%;height:100%}
.del-btn{position:absolute;top:3px;right:3px;width:20px;height:20px;background:rgba(0,0,0,.5);border-radius:50%;display:flex;align-items:center;justify-content:center}
.del-x{color:#fff;font-size:11px;font-weight:700}
.img-index{position:absolute;bottom:3px;left:3px;width:16px;height:16px;background:rgba(102,126,234,.8);border-radius:50%;display:flex;align-items:center;justify-content:center}
.img-index text{color:#fff;font-size:9px;font-weight:700}
.add-btn{width:80px;height:80px;border-radius:12px;border:2px dashed #ddd;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#fafafa}
.add-icon{font-size:28px;color:#bbb;line-height:1}
.add-txt{font-size:11px;color:#bbb;margin-top:2px}
/* 提交 */
.submit-area{margin:6px 15px 20px}
.submit-btn{width:100%;height:52px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:16px;color:#fff;font-size:17px;font-weight:700;border:none;box-shadow:0 4px 15px rgba(102,126,234,.35)}
.submit-btn::after{border:none}
.submit-btn[disabled]{opacity:.5;box-shadow:none}
.btn-text{color:#fff}
.submit-info{display:flex;align-items:center;justify-content:center;margin-top:12px;gap:4px}
.info-icon{font-size:12px;color:#bbb}
.info-text{font-size:11px;color:#bbb}
</style>