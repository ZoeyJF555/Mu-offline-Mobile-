class SoundEngine {
  private ctx: AudioContext | null = null;
  public muted = false;

  private ensureContext(): AudioContext | null {
    if (this.muted || typeof window === 'undefined') return null;
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
    return this.ctx;
  }

  private playTone(freq: number, endFreq: number, duration: number, type: OscillatorType, volume = 0.08, delay = 0) {
    const ctx = this.ensureContext();
    if (!ctx) return;
    const now = ctx.currentTime + delay;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, now);
    osc.frequency.exponentialRampToValueAtTime(Math.max(30, endFreq), now + duration);
    gain.gain.setValueAtTime(volume, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + duration);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(now);
    osc.stop(now + duration);
  }

  playAttack(kind: 'dk' | 'dw' | 'elf') {
    if (kind === 'dk') this.playTone(220, 75, 0.11, 'sawtooth', 0.06);
    else if (kind === 'dw') this.playTone(540, 210, 0.14, 'sine', 0.06);
    else this.playTone(680, 320, 0.09, 'triangle', 0.05);
  }

  playSkill() {
    this.playTone(340, 820, 0.18, 'triangle', 0.07);
    this.playTone(220, 110, 0.22, 'sawtooth', 0.05, 0.05);
  }

  playJewelDrop() {
    this.playTone(1046, 1318, 0.12, 'sine', 0.09, 0);
    this.playTone(1318, 1567, 0.16, 'sine', 0.09, 0.09);
  }

  playPotion() {
    this.playTone(420, 660, 0.12, 'sine', 0.06);
  }

  playLevelUp() {
    this.playTone(523, 523, 0.12, 'triangle', 0.08, 0);
    this.playTone(659, 659, 0.12, 'triangle', 0.08, 0.1);
    this.playTone(784, 1046, 0.26, 'triangle', 0.09, 0.2);
  }

  playChaosSuccess() {
    this.playTone(440, 880, 0.15, 'sine', 0.09, 0);
    this.playTone(660, 1320, 0.25, 'sine', 0.1, 0.12);
  }

  playChaosFail() {
    this.playTone(240, 90, 0.35, 'sawtooth', 0.08, 0);
  }
}

export const soundFx = new SoundEngine();
