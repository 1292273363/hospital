const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    loading: false,
    chatList: [],
    replyDraft: {},
    submittingId: null
  },

  onShow() {
    this.loadChats();
  },

  normalizeUrl(url) {
    if (!url) return '';
    return url.startsWith('/') ? `${app.globalData.baseUrl}${url}` : url;
  },

  async loadChats() {
    this.setData({ loading: true });
    try {
      const result = await request({
        url: '/api/consult/doctor/chats',
        method: 'GET',
        showLoading: false
      });
      const list = (result.data || []).map(item => ({
        ...item,
        scarImageUrl: this.normalizeUrl(item.scarImageUrl)
      }));
      this.setData({ chatList: list });
    } catch (err) {
      console.error('加载医生沟通列表失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onReplyInput(e) {
    const id = e.currentTarget.dataset.id;
    const value = e.detail.value || '';
    const replyDraft = { ...this.data.replyDraft, [id]: value };
    this.setData({ replyDraft });
  },

  async submitReply(e) {
    const id = e.currentTarget.dataset.id;
    const original = this.data.chatList.find(item => item.id === id);
    const reply = ((this.data.replyDraft[id] || '').trim()) || ((original && original.doctorReply) ? original.doctorReply.trim() : '');
    if (!reply) {
      wx.showToast({ title: '请输入回复内容', icon: 'none' });
      return;
    }

    this.setData({ submittingId: id });
    try {
      await request({
        url: '/api/consult/doctor/reply',
        method: 'POST',
        data: {
          consultationId: id,
          reply
        },
        showLoading: false
      });
      wx.showToast({ title: '回复已保存', icon: 'success' });
      this.loadChats();
    } catch (err) {
      console.error('保存医生回复失败', err);
    } finally {
      this.setData({ submittingId: null });
    }
  }
});
