export const motionEase = [0.16, 1, 0.3, 1]

export const inViewOnce = {
  once: true,
  margin: '0px 0px -80px 0px',
  amount: 0.16
}

export const revealInitial = {
  opacity: 0,
  y: 30,
  filter: 'blur(6px)'
}

export const revealInView = {
  opacity: 1,
  y: 0,
  filter: 'blur(0px)'
}

export const cardInitial = {
  opacity: 0,
  y: 34,
  scale: 0.96,
  filter: 'blur(6px)'
}

export const cardInView = {
  opacity: 1,
  y: 0,
  scale: 1,
  filter: 'blur(0px)'
}

export const cardExit = {
  opacity: 0,
  y: 22,
  scale: 0.96,
  filter: 'blur(6px)'
}

export const revealTransition = {
  duration: 0.62,
  ease: motionEase
}

export const cardTransition = (index = 0, baseDelay = 0) => ({
  duration: 0.62,
  delay: baseDelay + index * 0.055,
  ease: motionEase
})

export const softSpring = {
  type: 'spring',
  stiffness: 420,
  damping: 34
}

export const pageInitial = {
  opacity: 0,
  y: 18,
  filter: 'blur(8px)'
}

export const pageAnimate = {
  opacity: 1,
  y: 0,
  filter: 'blur(0px)'
}

export const pageExit = {
  opacity: 0,
  y: -12,
  filter: 'blur(8px)'
}

export const pageTransition = {
  duration: 0.34,
  ease: motionEase
}
