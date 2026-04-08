Page({
  data: {
    patient: {
      name: '张女士',
      statusText: '在线 · 面部隆鼻咨询',
      avatar: 'https://design.gemcoder.com/staticResource/echoAiSystemImages/735de1c3faee17e0f1f396277bd423cb.png'
    },
    doctorAvatar:
      'https://design.gemcoder.com/staticResource/echoAiSystemImages/3787c1b7103c2a054dcc6d92322fec36.png',
    messages: [],
    draft: '',
    nextId: 1,
    scrollIntoView: ''
  },

  onShow() {
    if (!this.data.messages.length) {
      this.initMockMessages();
    } else {
      this.scrollToBottom();
    }
  },

  initMockMessages() {
    const msgs = [
      {
        id: 1,
        from: 'patient',
        type: 'text',
        content:
          '医生您好，我想咨询一下隆鼻手术的相关事宜。我对自己的鼻子形状不太满意，想了解一下手术相关的信息。',
        time: '09:12'
      },
      {
        id: 2,
        from: 'doctor',
        type: 'text',
        content:
          '你好张女士，很高兴为您服务。可以和我说一下你具体想要改善哪些方面吗？我可以给你提供专业的建议。',
        time: '09:15'
      },
      {
        id: 3,
        from: 'patient',
        type: 'image',
        imageUrl:
          'https://design.gemcoder.com/staticResource/echoAiSystemImages/77a5a962b7f2367a1fcdc28c8f7b8545.png',
        time: '09:18'
      },
      {
        id: 4,
        from: 'patient',
        type: 'text',
        content:
          '这是我的正面照片，我主要觉得鼻梁太低了，想垫高一些，您觉得适合什么类型的假体呢？',
        time: '09:18'
      },
      {
        id: 5,
        from: 'doctor',
        type: 'text',
        content:
          '从照片来看，你的基础条件还不错。根据你的面部轮廓和需求，我比较推荐使用硅胶假体或者膨体，这两种都是目前比较安全成熟的材料。你之前有了解过吗？',
        time: '09:22'
      }
    ];
    this.setData({
      messages: msgs,
      nextId: msgs.length + 1,
      scrollIntoView: 'msg-5'
    });
  },

  scrollToBottom() {
    const lastId = this.data.nextId - 1;
    if (lastId <= 0) return;
    this.setData({ scrollIntoView: `msg-${lastId}` });
  },

  onInput(e) {
    this.setData({ draft: e.detail.value || '' });
  },

  onSend() {
    const text = (this.data.draft || '').trim();
    if (!text) {
      wx.showToast({ title: '请输入消息', icon: 'none' });
      return;
    }
    const now = new Date();
    const h = `${now.getHours()}`.padStart(2, '0');
    const m = `${now.getMinutes()}`.padStart(2, '0');
    const time = `${h}:${m}`;
    const newMsg = {
      id: this.data.nextId,
      from: 'doctor',
      type: 'text',
      content: text,
      time
    };
    const messages = this.data.messages.concat(newMsg);
    this.setData(
      {
        messages,
        draft: '',
        nextId: this.data.nextId + 1,
        scrollIntoView: `msg-${newMsg.id}`
      }
    );
  },

  onViewPatientDetail() {
    wx.navigateTo({
      url: '/pages/doctor/patient-detail?id=1'
    });
  }
});
